package com.ackpdfbox.app;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.PDPage;

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;

public class FieldPositionDeltaMapGenerator {
  public PDDocument sourcePdf;
  public PDDocument targetPdf;

  public String sourcePdfPath;
  public String targetPdfPath;
  public String jsonOutPath;

  public FieldPositionDeltaMapGenerator(String sourcePdfPath, String targetPdfPath, String jsonOutPath){
    this.sourcePdfPath = sourcePdfPath;
    this.targetPdfPath = targetPdfPath;
    this.jsonOutPath = jsonOutPath;
  }

  public void execute() throws IOException {
    this.sourcePdf = PDDocument.load(new File(this.sourcePdfPath));
    this.targetPdf = PDDocument.load(new File(this.targetPdfPath));

    try{
      String deltaMapJsonString = getDeltaMap();

      FileOutputStream outputStream = new FileOutputStream(this.jsonOutPath);
      outputStream.write(deltaMapJsonString.getBytes());
      outputStream.close();
    }finally{
      if (this.sourcePdf != null){
        this.sourcePdf.close();
      }

      if (this.targetPdf != null){
        this.targetPdf.close();
      }
    }
  }

  /** final method used by App to get String of all fields. Also ensures PDF read is closed in error */
  public String getDeltaMap() throws IOException{
    JsonArray fieldPositionDeltaMapArray = new JsonArray();
    HashMap<String, PDField> sourcePdfFields = createFieldHashMap(this.sourcePdf.getDocumentCatalog().getAcroForm());
    HashMap<String, PDField> targetPdfFields = createFieldHashMap(this.targetPdf.getDocumentCatalog().getAcroForm());

    Iterator it = sourcePdfFields.entrySet().iterator();
    while(it.hasNext()) {
      Map.Entry<String, PDField> entry = (Map.Entry<String, PDField>)it.next();
      PDField correspondingField = (PDField) targetPdfFields.get(entry.getKey());

      if (correspondingField == null) {
        continue;
      }

      JsonObject deltaObject = getDeltaObject(entry.getValue(), correspondingField);
      if (deltaObject.getAsJsonArray("widgetDeltas").size() > 0) {
        fieldPositionDeltaMapArray.add(deltaObject);
      }
    }

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(fieldPositionDeltaMapArray);
  }

  private JsonObject getDeltaObject(PDField sourceField, PDField targetField){
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("fullyQualifiedName", sourceField.getFullyQualifiedName());

    JsonArray widgetDeltasArray = new JsonArray();
    for (int i = 0; i < sourceField.getWidgets().size(); i++) {
      PDRectangle sourceRectangle = sourceField.getWidgets().get(i).getRectangle();
      PDRectangle targetRectangle = targetField.getWidgets().get(i).getRectangle();

      Float lowerLeftXDelta = targetRectangle.getLowerLeftX() - sourceRectangle.getLowerLeftX();
      Float lowerLeftYDelta = targetRectangle.getLowerLeftY() - sourceRectangle.getLowerLeftY();
      Float upperRightXDelta = targetRectangle.getUpperRightX() - sourceRectangle.getUpperRightX();
      Float upperRightYDelta = targetRectangle.getUpperRightY() - sourceRectangle.getUpperRightY();

      if ((lowerLeftXDelta + lowerLeftYDelta + upperRightXDelta + upperRightYDelta) == 0) {
        // no need to store the delta in the map
        continue;
      }

      JsonObject widgetDeltaObject = new JsonObject();
      widgetDeltaObject.addProperty("lowerLeftXDelta", lowerLeftXDelta);
      widgetDeltaObject.addProperty("lowerLeftYDelta", lowerLeftYDelta);
      widgetDeltaObject.addProperty("upperRightXDelta", upperRightXDelta);
      widgetDeltaObject.addProperty("upperRightYDelta", upperRightYDelta);

      widgetDeltasArray.add(widgetDeltaObject);
    }

    jsonObject.add("widgetDeltas", widgetDeltasArray);
    return jsonObject;
  }

  public HashMap<String, PDField> createFieldHashMap (PDAcroForm acroForm) throws IOException {
    HashMap<String, PDField> fields = new HashMap<>();

    //inspect field values
    for (PDField field : acroForm.getFields()) {
      addFieldToMap(fields, field);
    }

    return fields;
  }

  public void addFieldToMap (HashMap<String, PDField> fields, PDField field) throws IOException {
    fields.put(field.getFullyQualifiedName(), field);

    if (field instanceof PDNonTerminalField) {
      // this field has children
      for (PDField child : ((PDNonTerminalField)field).getChildren()) {
        addFieldToMap(fields, child);
      }
    }
  }
}
