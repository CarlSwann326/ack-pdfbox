package com.ackpdfbox.app;

import java.io.IOException;
import java.util.ArrayList;

import com.ackpdfbox.app.AddImage;
import com.ackpdfbox.app.FieldReader;
import com.ackpdfbox.app.FieldFiller;
import com.ackpdfbox.app.FieldCopier;
import com.ackpdfbox.app.FieldRenamer;
import com.ackpdfbox.app.FieldTranslator;

import com.ackpdfbox.app.Decrypt;
import com.ackpdfbox.app.Encrypt;
import com.ackpdfbox.app.CreateSignature;
import org.apache.pdfbox.util.Version;
import org.apache.tools.ant.types.Commandline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class App{
  /**
   * This will read a PDF file and print out the form elements. <br>
   * see usage() for commandline
   *
   * @param args command line arguments
   *
   * @throws IOException If there is an error importing the FDF document.
   */
  public static void main(String[] args) throws IOException{
    if (args.length<1){
      usage();
    }else{
      String command = args[0];
      String[] arguments = new String[args.length - 1];
      System.arraycopy(args, 1, arguments, 0, arguments.length);

      switch( command.toLowerCase() ){
        case "combo":
          for( int i=1; i<args.length; i++ ){
            String subCommand = args[i];
            //System.out.println( "subCommand:" + subCommand );
            String myArgs[] = Commandline.translateCommandline(subCommand);
            main(myArgs);
          }
          break;

        case "read":
          FieldReader fieldReader = new FieldReader();
          fieldReader.loadPdfByPath( args[1] );
          String json = fieldReader.getFields();
          if(args.length>2){
            fieldReader.write(json, args[2]);
          }else{
            System.out.println( json );
          }
          break;

        case "fill":
          if(args.length < 4){
            fillUsage();
          }else{
            FieldFiller fieldFiller = new FieldFiller(args[1], args[2], args[3]);

            //add options
            for( int i=1; i<args.length; i++ ){
              String key = args[i];
              if( key.equals( "-flatten" ) ){
                fieldFiller.flatten = Boolean.parseBoolean( args[++i] );
              }
            }

            fieldFiller.execute();
          }
          break;

        case "copy-fields":
          if(args.length < 4){
            copyFieldsUsage();
          }else{
            FieldCopier fieldCopier = new FieldCopier(args[1], args[2], args[3]);
            fieldCopier.execute();
          }
          break;

        case "rename-fields":
          if(args.length < 5){
            renameFieldsUsage();
          }else{
            FieldRenamer fieldRenamer = new FieldRenamer(args[1], args[2], args[3], args[4]);
            fieldRenamer.execute();
          }
          break;

        case "translate-fields":
          if(args.length < 5){
            translateFieldsUsage();
          }else{
            FieldTranslator fieldTranslator = new FieldTranslator(args[1], args[2], args[3], args[4]);
            fieldTranslator.execute();
          }
          break;

        case "add-image":
          com.ackpdfbox.app.AddImage.main(args);
          break;

        case "encrypt":
          addBouncyCastle();
          try{
            com.ackpdfbox.app.Encrypt.main(arguments);
          }catch(Exception e){
            System.err.println("Error: Cannot encrypt file: "+arguments[0]);
            e.printStackTrace();
          }
          break;

        case "decrypt":
          addBouncyCastle();
          try{
            com.ackpdfbox.app.Decrypt.main(arguments);
          }catch(Exception e){
            System.err.println("Error: Cannot decrypt file: "+arguments[0]);
            e.printStackTrace();
          }
          break;

        case "sign":
          try{
            com.ackpdfbox.app.CreateSignature.main(arguments);
          }catch(Exception e){
            System.err.println("Error: Cannot sign file: "+arguments[0]);
            e.printStackTrace();
          }
          break;

        case "pdftoimage":
          try{
            Gson gson = new GsonBuilder().create();
            ArrayList<String> result = com.ackpdfbox.app.PDFToImage.main(arguments);
            System.out.println( gson.toJson( result ) );
          }catch(Exception e){
            System.err.println( "Error: Cannot create image of file: "+arguments[0] );
            e.printStackTrace();
          }
          break;

        case "-version":
          String version = org.apache.pdfbox.util.Version.getVersion();
          if (version != null){
              System.out.println("PDFBox version: \""+version+"\"");
          }else{
            System.out.println("unknown");
          }
          break;

        default:usage();
      }
    }
  }

  public static void addBouncyCastle(){
    Security.addProvider(new BouncyCastleProvider());
  }

  private static void usage(){
    System.err.println("usage: <read|fill|pdftoimage|add-image|copy-fields|rename-fields|translate-fields|encrypt|decrypt|-version>");
  }

  private static void fillUsage(){
    System.err.println("usage: fill <pdf-path> <json-path> <out-path>");
  }

  private static void copyFieldsUsage(){
    System.err.println("usage: copy-fields <source-pdf-path> <target-pdf-path> <out-path>");
  }

  private static void renameFieldsUsage(){
    System.err.println("usage: rename-fields <source-pdf-path> <out-path> <source-string> <target-string>");
  }

  private static void translateFieldsUsage(){
    System.err.println("usage: translate-fields <source-pdf-path> <out-path> <translate-x> <translate-y>");
  }

  private static void addImageUsage(){
    System.err.println("usage: add-image <pdf-path> <image-path> <page> <x> <y> <out-path>");
  }
}
