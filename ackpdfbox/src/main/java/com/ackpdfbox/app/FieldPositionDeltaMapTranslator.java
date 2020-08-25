package com.ackpdfbox.app;

import java.io.IOException;
import java.io.File;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.FileReader;
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

public class FieldPositionDeltaMapTranslator {
  public PDDocument sourcePdf;

  public String sourcePdfPath;
  public String deltaMapPath;
  public String outPath;

  public FieldPositionDeltaMapTranslator(String sourcePdfPath, String deltaMapPath, String outPath){
    this.sourcePdfPath = sourcePdfPath;
    this.deltaMapPath = deltaMapPath;
    this.outPath = outPath;
  }

  public void execute() throws IOException {
    this.sourcePdf = PDDocument.load(new File(this.sourcePdfPath));

    HashMap<String, PDField> sourcePdfFields = createFieldHashMap(this.sourcePdf.getDocumentCatalog().getAcroForm());
    JsonArray deltaObjectsArray = new Gson().fromJson(new FileReader(this.deltaMapPath), JsonArray.class);

    for (int i = 0; i < deltaObjectsArray.size(); i++) {
      JsonObject deltaObject = (JsonObject) deltaObjectsArray.get(i);
      PDField correspondingField = (PDField) sourcePdfFields.get(deltaObject.get("fullyQualifiedName").getAsString());

      JsonArray widgetDeltasArray = deltaObject.getAsJsonArray("widgetDeltas");

      for (int j = 0; j < widgetDeltasArray.size(); j++) {
        JsonObject widgetDetlaObject = widgetDeltasArray.get(j).getAsJsonObject();
        PDRectangle rectangle = correspondingField.getWidgets().get(j).getRectangle();

        rectangle.setLowerLeftX(rectangle.getLowerLeftX() + widgetDetlaObject.get("lowerLeftXDelta").getAsFloat());
        rectangle.setLowerLeftY(rectangle.getLowerLeftY() + widgetDetlaObject.get("lowerLeftYDelta").getAsFloat());
        rectangle.setUpperRightX(rectangle.getUpperRightX() + widgetDetlaObject.get("upperRightXDelta").getAsFloat());
        rectangle.setUpperRightY(rectangle.getUpperRightY() + widgetDetlaObject.get("upperRightYDelta").getAsFloat());

        correspondingField.getWidgets().get(j).setRectangle(rectangle);
      }
    }

    this.sourcePdf.save(this.outPath);
    this.sourcePdf.close();
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
