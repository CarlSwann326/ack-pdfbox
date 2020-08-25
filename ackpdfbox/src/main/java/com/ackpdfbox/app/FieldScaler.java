package com.ackpdfbox.app;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.fdf.FDFDocument;
import org.apache.pdfbox.pdmodel.fdf.FDFField;
import org.apache.pdfbox.pdmodel.fdf.FDFDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;


public class FieldScaler {
  public PDDocument pdf;

  public String sourcePdfPath;
  public String outputPdfPath;
  public String fieldName;
  public String translationDefinition;

  public String lowerLeftXTranslation;
  public String lowerLeftYTranslation;
  public String upperRightXTranslation;
  public String upperRightYTranslation;

  public FieldScaler(String sourcePdfPath, String outputPdfPath, String fieldName, String translationDefinition){
    this.sourcePdfPath = sourcePdfPath;
    this.fieldName = fieldName;
    this.translationDefinition = translationDefinition;
    this.outputPdfPath = outputPdfPath;

    String[] points = translationDefinition.split(",");
    this.lowerLeftXTranslation = points[0];
    this.lowerLeftYTranslation = points[1];
    this.upperRightXTranslation = points[2];
    this.upperRightYTranslation = points[3];
  }

  public void execute() throws IOException {
    PDDocument sourcePdf = PDDocument.load(new File(this.sourcePdfPath));
    PDAcroForm sourcePdfAcroForm = sourcePdf.getDocumentCatalog().getAcroForm();

    List<PDField> fields = sourcePdfAcroForm.getFields();

    //inspect field values
    for (PDField field : fields) {
      processField(field);
    }

    sourcePdf.save(this.outputPdfPath);
    sourcePdf.close();
  }


  private void processField(PDField field) throws IOException {
    String fullName = field.getFullyQualifiedName();

    if (this.fieldName.equals(fullName)) {
      PDRectangle rectangle = field.getWidgets().get(0).getRectangle();
      rectangle.setLowerLeftX(rectangle.getLowerLeftX() + Float.parseFloat(this.lowerLeftXTranslation));
      rectangle.setLowerLeftY(rectangle.getLowerLeftY() + Float.parseFloat(this.lowerLeftYTranslation));
      rectangle.setUpperRightX(rectangle.getUpperRightX() + Float.parseFloat(this.upperRightXTranslation));
      rectangle.setUpperRightY(rectangle.getUpperRightY() + Float.parseFloat(this.upperRightYTranslation));
      field.getWidgets().get(0).setRectangle(rectangle);
    }


    if (field instanceof PDNonTerminalField) {
      // this field has children
      for (PDField child : ((PDNonTerminalField)field).getChildren()) {
        processField(child);
      }
    }
  }
}
