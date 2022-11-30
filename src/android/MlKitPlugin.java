package com.outsystems.mlkitplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Point;

import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentificationOptions;
import com.google.firebase.ml.naturallanguage.languageid.IdentifiedLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class MlKitPlugin extends CordovaPlugin {
    private static final String TAG = "MlKitPlugin";
    private static final int TAKE_PICTURE_REQUEST_CODE = 1;
    private static final int OPEN_GALLERY_REQUEST_CODE = 2;

    private static Context context;

    private CallbackContext _callbackContext;

    private JSONObject options = new JSONObject();

    private Actions myAction;

    private File tempImage;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getActivity().getApplicationContext();
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext)
            throws JSONException {
        _callbackContext = callbackContext;
        String error = isValidCall(action,args);
        try{
            if(error.isEmpty()){
                myAction = Actions.fromString(action);
                switch(myAction){
                    case GETTEXT:
                    case GETLABLE:
                    case GETFACE:
                        options = args.getJSONObject(0);

                        if (options.optBoolean("TakePicture",false)) {
                            try {
                                requestPermission(Manifest.permission.CAMERA, 1);
                            }catch(Exception e){
                                callbackContext.error("Exception occurred on requestPermission!\n"
                                        +e.getMessage());
                            }
                        } else {
                            try {
                                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                                        1);
                            }catch(Exception e){
                                callbackContext.error("Exception occurred on requestPermission!\n"
                                        +e.getMessage());
                            }
                        }
                        break;
                    case IDENTIFYLANG:
                        options = args.optJSONObject(0);
                        Double confidence = options.optDouble("confidence",0.5);
                        FirebaseLanguageIdentificationOptions identifierOptions;
                        identifierOptions = new FirebaseLanguageIdentificationOptions.Builder().setConfidenceThreshold(confidence.floatValue()).build();
                        runTextIdentifier(callbackContext,options.optString("text",""),identifierOptions);
                        break;

                    case REPLY:
                        options = args.optJSONObject(0);
                        if (options.has("Identifier")) {
                            String identifier = options.optString("Identifier", "");
                            if (conversations.containsKey(identifier)) {
                                reply(callbackContext, conversations.get(identifier));
                            } else {
                                callbackContext.error("Conversation Identifier does not Exist!");
                            }
                        }else{
                            callbackContext.error("Options Identifier propriety not found!");
                        }
                        break;
                    case ADDMESSAGES:
                        options = args.optJSONObject(0);

                        if (options.has("Identifier")&& options.has("Messages")){
                            Message[] messages = convertJsonToMessages(options.getJSONArray("Messages"));
                            String identifier = options.getString("Identifier");
                            addMessages(callbackContext,messages,identifier);
                        }else{
                            callbackContext.error("Options Identifier or Messages propriety not found!");
                        }
                        break;
                    case REMOVEMESSAGE:
                        options = args.optJSONObject(0);
                        if (options.has("Identifier")&& options.has("MessageId")){
                            removeMessage(options.getString("Identifier"),options.getInt("MessageId"));
                        }else{
                            callbackContext.error("Options Identifier or Id propriety not found!");
                        }
                        break;
                }
                return true;
            }else{
                callbackContext.sendPluginResult(
                        new PluginResult(PluginResult.Status.INVALID_ACTION,error));
            }
        }catch(Exception e){
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
        return false;
    }


    private String isValidCall(String action, JSONArray args) {
        boolean isValid;
        Actions a = Actions.fromString(action);
        switch (a) {
            case GETTEXT:
            case GETLABLE:
            case GETFACE:
            case IDENTIFYLANG:
            case REPLY:
            case ADDMESSAGES:
            case REMOVEMESSAGE:
                isValid = args.length() == 1;
                try{
                    args.getJSONObject(0);
                } catch (JSONException e) {
                    isValid = false;
                }
                return isValid ? "" : a.argsDesc;
            default:
                return Actions.INVALID.argsDesc;
        }
    }

    enum Actions {
        INVALID("", "Invalid action"),
        GETTEXT("getText","Invalid arguments-> options: JSONObject"),
        GETLABLE("getLabel","Invalid arguments-> options: JSONObject"),
        GETFACE("getFace","Invalid arguments-> options: JSONObject"),
        IDENTIFYLANG("identifyLang","Invalid arguments-> options: JSONObject"),
        REPLY("reply","Invalid arguments-> options: JSONObject"),
        ADDMESSAGES("addMessage","Invalid arguments-> options: JSONObject"),
        REMOVEMESSAGE("removeMessage","Invalid arguments-> options: JSONObject");

        String name;
        String argsDesc;

        Actions(String name, String argsDesc) {
            this.name = name;
            this.argsDesc = argsDesc;
        }

        public static Actions fromString(String action) {
            for (Actions a : Actions.values()) {
                if (a.name.equalsIgnoreCase(action)) {
                    return a;
                }
            }
            return INVALID;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    //                               Text Recognition                             //
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private void runTextRecognition(final CallbackContext callbackContext, final Bitmap img,
                                    final String language, final Boolean onCloud) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(img);

        FirebaseVisionTextRecognizer textRecognizer;

        if(onCloud) {
            if (!language.isEmpty()) {
                FirebaseVisionCloudTextRecognizerOptions options =
                        new FirebaseVisionCloudTextRecognizerOptions.Builder()
                            .setLanguageHints(Arrays.asList(language)).build();

                textRecognizer= FirebaseVision.getInstance().getCloudTextRecognizer(options);
            } else {
                textRecognizer = FirebaseVision.getInstance().getCloudTextRecognizer();
            }
        } else {
            textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        }

        textRecognizer.processImage(image).addOnSuccessListener(texts -> {
            try {
                JSONObject json = new JSONObject();
                JSONArray blocks = new JSONArray();

                json.put("text", texts.getText());
                json.put("textBlocks", blocks);

                for (FirebaseVisionText.TextBlock block : texts.getTextBlocks()) {
                    Log.d(TAG, block.getText());
                    JSONObject oBlock = new JSONObject();
                    JSONArray lines = new JSONArray();
                    oBlock.put("text", block.getText());
                    oBlock.put("confidence", block.getConfidence());
                    oBlock.put("boundingBox", rectToJson(block.getBoundingBox()));
                    oBlock.put("cornerPoints", pointsToJson(block.getCornerPoints()));
                    oBlock.put("lines", lines);
                    blocks.put(oBlock);

                    for (FirebaseVisionText.Line line : block.getLines()) {
                        JSONObject oLine = new JSONObject();
                        oLine.put("text", line.getText());
                        oLine.put("confidence", line.getConfidence());
                        oLine.put("boundingBox", rectToJson(line.getBoundingBox()));
                        oLine.put("cornerPoints", pointsToJson(line.getCornerPoints()));
                        lines.put(oLine);
                    }
                }
                callbackContext.success(json.toString());
            } catch(JSONException e) {
                e.printStackTrace();
                callbackContext.error(e.getMessage());
            }
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        });

    }

    private FirebaseVisionTextRecognizer getTextRecognitionDevice() {
        return FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }

    private FirebaseVisionTextRecognizer getTextRecognitionCloud( final String language) {
        if (!language.isEmpty()) {
            FirebaseVisionCloudTextRecognizerOptions options =
                    new FirebaseVisionCloudTextRecognizerOptions.Builder()
                        .setLanguageHints(Arrays.asList(language)).build();

            return FirebaseVision.getInstance()
                    .getCloudTextRecognizer(options);
        } else {
            return FirebaseVision.getInstance().getCloudTextRecognizer();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    //                             Label Identification                           //
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////


    private void runLabelRecognition(final CallbackContext callbackContext,
                                     Bitmap img,Boolean onCloud) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(img);
        FirebaseVisionImageLabeler labeler;
        if (onCloud){
            labeler = FirebaseVision.getInstance().getCloudImageLabeler();
        }else{
            labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();
        }

        labeler.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                        JSONArray json = new JSONArray();

                        for (FirebaseVisionImageLabel label: labels){
                            try {

                                String text = label.getText();
                                String entityId = label.getEntityId();
                                float confidence = label.getConfidence();

                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("text", text);
                                jsonObject.put("entityId", entityId);
                                jsonObject.put("confidence", confidence);

                                json.put(jsonObject);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        callbackContext.success(json.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                });
    }


    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    //                              Face Detection                                //
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private void runFaceDetector(final CallbackContext callbackContext,Bitmap img,
                                 final FirebaseVisionFaceDetectorOptions faceOptions){

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(img);

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(faceOptions);

        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                // Task completed successfully
                                // ...

                                JSONArray json = new JSONArray();

                                for (FirebaseVisionFace face : faces) {
                                    try {
                                        JSONObject faceJson = new JSONObject();

                                        Rect bounds = face.getBoundingBox();

                                        faceJson.put("Bounds", rectToJson(bounds));

                                        // Head is rotated to the right rotY degrees
                                        faceJson.put("RotY",face.getHeadEulerAngleY());
                                        // Head is tilted sideways rotZ degrees
                                        faceJson.put("RotZ",face.getHeadEulerAngleZ());

                                        // If landmark detection was enabled (mouth, ears, eyes,
                                        // cheeks, and nose available):
                                        if (faceOptions.getLandmarkMode() ==
                                                FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS) {
                                            JSONArray landmarks = new JSONArray();


                                            List<Integer> landmarksValues =
                                                    Arrays.asList(
                                                        FirebaseVisionFaceLandmark.MOUTH_BOTTOM
                                                        ,FirebaseVisionFaceLandmark.LEFT_CHEEK
                                                        ,FirebaseVisionFaceLandmark.LEFT_EAR
                                                        ,FirebaseVisionFaceLandmark.LEFT_EYE
                                                        ,FirebaseVisionFaceLandmark.MOUTH_LEFT
                                                        ,FirebaseVisionFaceLandmark.NOSE_BASE
                                                        ,FirebaseVisionFaceLandmark.RIGHT_CHEEK
                                                        ,FirebaseVisionFaceLandmark.RIGHT_EAR
                                                        ,FirebaseVisionFaceLandmark.RIGHT_EYE
                                                        ,FirebaseVisionFaceLandmark.MOUTH_RIGHT);

                                            for (int landmarkValue : landmarksValues){

                                                FirebaseVisionFaceLandmark landmark =
                                                        face.getLandmark(landmarkValue);
                                                if (landmark != null){
                                                    JSONObject landmarkJson =
                                                            faceLandmarkConvertToJson(landmark);
                                                    if (landmarkJson != null){
                                                        landmarks.put(landmarkJson);
                                                    }
                                                }
                                            }

                                            if (!landmarks.equals(new JSONArray())){
                                                faceJson.put("Landmarks",landmarks);
                                            }
                                        }

                                        // If contour detection was enabled:
                                        if (faceOptions.getContourMode() ==
                                                FirebaseVisionFaceDetectorOptions.ALL_CONTOURS){
                                            JSONArray contours = new JSONArray();


                                            List<Integer> contoursValues =
                                                Arrays.asList(
                                                    FirebaseVisionFaceContour.ALL_POINTS
                                                    ,FirebaseVisionFaceContour.FACE
                                                    ,FirebaseVisionFaceContour.LEFT_EYEBROW_TOP
                                                    ,FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM
                                                    ,FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP
                                                    ,FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM
                                                    ,FirebaseVisionFaceContour.LEFT_EYE
                                                    ,FirebaseVisionFaceContour.RIGHT_EYE
                                                    ,FirebaseVisionFaceContour.UPPER_LIP_TOP
                                                    ,FirebaseVisionFaceContour.UPPER_LIP_BOTTOM
                                                    ,FirebaseVisionFaceContour.LOWER_LIP_TOP
                                                    ,FirebaseVisionFaceContour.LOWER_LIP_BOTTOM
                                                    ,FirebaseVisionFaceContour.NOSE_BRIDGE
                                                    ,FirebaseVisionFaceContour.NOSE_BOTTOM);
                                            for (int contourValue : contoursValues){

                                                FirebaseVisionFaceContour contour =
                                                        face.getContour(contourValue);
                                                if (contour != null){
                                                    JSONObject contourJson =
                                                            faceContourConvertToJson(contour);
                                                    if (contourJson != null) {
                                                        contours.put(contourJson);
                                                    }
                                                }
                                            }
                                            if (!contours.equals(new JSONArray())){
                                                faceJson.put("Contours",contours);
                                            }
                                        }

                                        // If classification was enabled:
                                        if (faceOptions.getClassificationMode() == FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS){
                                            JSONObject classification = new JSONObject();
                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                classification.put("SmileProbability",face.getSmilingProbability());
                                            }
                                            if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                classification.put("LeftEyeOpenProbability",face.getLeftEyeOpenProbability());
                                            }
                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                classification.put("RightEyeOpenProbability",face.getRightEyeOpenProbability());
                                            }
                                            faceJson.put("Classification",classification);
                                        }

                                        // If face tracking was enabled:
                                        if (faceOptions.isTrackingEnabled()) {
                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                                                faceJson.put("TrackingId",face.getTrackingId());
                                            }
                                        }
                                        json.put(faceJson);
                                    }catch (JSONException e){
                                        e.printStackTrace();
                                    }
                                }
                                callbackContext.success(json.toString());
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                                callbackContext.error(e.getMessage());
                            }
                        });
    }
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    //                            Language Identifier                             //
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    public void runTextIdentifier(final CallbackContext callbackContext,String text,FirebaseLanguageIdentificationOptions options){

        FirebaseLanguageIdentification langIdentifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification(options);

        langIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener(new OnSuccessListener<List<IdentifiedLanguage>>() {
                    @Override
                    public void onSuccess(List<IdentifiedLanguage> languages) {
                        JSONArray json = new JSONArray();

                        for (IdentifiedLanguage language : languages) {
                            try {
                                JSONObject languageJson = new JSONObject();

                                languageJson.put("Confidence",String.valueOf(language.getConfidence()));
                                languageJson.put("Code",language.getLanguageCode());

                                json.put(languageJson);
                            }catch (JSONException e){
                                e.printStackTrace();
                            }
                        }
                        callbackContext.success(json.toString());
                        }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                });
    }

    
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    //                               Smart Reply                                  //
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private HashMap<String,List<FirebaseTextMessage>> conversations;

    class Message{
        String message;
        long timestamp;
        String PersonId;
    }

    private void addMessages(CallbackContext callbackContext,Message[] messages,String identifier) throws JSONException {
        for (Message message : messages){
            addMessage(callbackContext,message,identifier);
        }
        callbackContext.success(convertFirebaseTextMessageToJson(conversations.get(identifier)));
    }

    private void addMessage(CallbackContext callbackContext,Message message,String identifier){
        List<FirebaseTextMessage> conversation;
        if (conversations == null){
            conversations = new HashMap<>();
            conversation = new ArrayList<>();
        }else{
            if (conversations.containsKey(identifier)){
                conversation = conversations.get(identifier);
            }else{
                conversation = new ArrayList<>();
            }
        }
        if (message.PersonId != null){
            conversation.add(FirebaseTextMessage.createForRemoteUser(
                    message.message, message.timestamp,message.PersonId));
        }else {
            conversation.add(FirebaseTextMessage.createForLocalUser(
                    message.message, message.timestamp));
        }
        if (conversations.containsKey(identifier)){
            conversations.remove(identifier);
            conversations.put(identifier,conversation);
        }else {
            conversations.put(identifier,conversation);
        }
    }


    private void reply(final CallbackContext callbackContext,List<FirebaseTextMessage> conversation){
        FirebaseSmartReply smartReply = FirebaseNaturalLanguage.getInstance().getSmartReply();
        smartReply.suggestReplies(conversation)
                .addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
                    @Override
                    public void onSuccess(SmartReplySuggestionResult result) {
                        if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                            // The conversation's language isn't supported, so the
                            // the result doesn't contain any suggestions.
                            callbackContext.error("No results");
                        } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                            JSONArray json = new JSONArray();

                            for (SmartReplySuggestion suggestion : result.getSuggestions()) {
                                try {
                                    JSONObject replyJson = new JSONObject();

                                    replyJson.put("Confidence",String.valueOf(suggestion.getConfidence()));
                                    replyJson.put("Reply",suggestion.getText());

                                    json.put(replyJson);
                                }catch (JSONException e){
                                    e.printStackTrace();
                                }
                            }
                            callbackContext.success(json.toString());
                            // Task completed successfully
                            // ...
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                });
    }

    private void removeMessage(String identifier,int id) throws JSONException {
        if (conversations.containsKey(identifier)) {
            if (conversations.get(identifier).size()>id && id >-1) {
                conversations.get(identifier).remove(id);
                _callbackContext.success(convertFirebaseTextMessageToJson(conversations.get(identifier)));
            }else {
                _callbackContext.error("Message id doesn't exist!");
            }
        }else{
            _callbackContext.error("Conversation Identifier does not exist!");
        }
    }

    private String convertFirebaseTextMessageToJson(List<FirebaseTextMessage> messages) throws JSONException {
        JSONArray json = new JSONArray();
        for (FirebaseTextMessage message : messages){
            JSONObject messageJson = new JSONObject();
            messageJson.put("message",message.zzda());
            messageJson.put("timestamp",message.getTimestampMillis());
            json.put(messageJson);
        }
        return json.toString();
    }

    private Message[] convertJsonToMessages(JSONArray json) throws JSONException {
        Message[] messages = new Message[json.length()];
        for (int idx = 0; idx < json.length(); idx++){
            JSONObject message = json.getJSONObject(idx);
            if (message.has("message") && message.has("timestamp")) {
                messages[idx] = new Message();
                messages[idx].message = message.getString("message");
                messages[idx].timestamp = message.getLong("timestamp");
                if (message.has("personId")) {
                    messages[idx].PersonId = message.getString("personId");
                }
            }else{
                _callbackContext.error("Message Format error.\nMessage or timestamp not found!");
                return null;
            }
        }
        return messages;

    }

    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    //                                  Camera                                    //
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private void takePicture(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final String file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/MlKitPhoto.jpg";
        File newFile = new File(file);
        try{
            newFile.createNewFile();
        }catch(IOException e){

        }
        tempImage = newFile;
        Activity act = this.cordova.getActivity();
        String packagename = act.getComponentName().getPackageName();
        Uri imageUri = FileProvider.getUriForFile(
                act.getApplicationContext(),
                packagename,
                tempImage);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);

        if (this.cordova != null) {
            PackageManager mPm = act.getPackageManager();
            if(takePictureIntent.resolveActivity(mPm) != null)
            {
                this.cordova.startActivityForResult((CordovaPlugin) this, takePictureIntent, TAKE_PICTURE_REQUEST_CODE);
            }
            else
            {
                Log.d(TAG, "Error: You don't have a default camera.  Your device may not be CTS complaint.");
            }
        }
    }

    private void openPhotoLibrary(){
        Intent PictureIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        if (this.cordova != null) {
            // Let's check to make sure the camera is actually installed. (Legacy Nexus 7 code)
            PackageManager mPm = this.cordova.getActivity().getPackageManager();
            if (PictureIntent.resolveActivity(mPm) != null) {
                this.cordova.startActivityForResult((CordovaPlugin) this, PictureIntent, OPEN_GALLERY_REQUEST_CODE);
            } else {
                Log.d(TAG, "Error: You don't have a default camera.  Your device may not be CTS complaint.");
            }
        }
    }

    protected void requestPermission(String permission, int requestId) throws Exception {
        Boolean granted = hasPermission(permission);
        if (granted) {
            if (permission.equals(Manifest.permission.CAMERA)) {
                takePicture();
            } else if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                openPhotoLibrary();
            }
        } else {
            try {
                java.lang.reflect.Method method = cordova.getClass().getMethod("requestPermission", org.apache.cordova.CordovaPlugin.class, int.class, java.lang.String.class);
                method.invoke(cordova, this, requestId, permission);
            } catch (NoSuchMethodException e) {
                throw new Exception("requestPermissions() method not found in CordovaInterface implementation of Cordova v" + CordovaWebView.CORDOVA_VERSION);
            }
        }
    }

    protected boolean hasPermission(String permission) throws Exception {
        boolean hasPermission = true;
        Method method;
        try {
            method = cordova.getClass().getMethod("hasPermission", permission.getClass());
            Boolean bool = (Boolean) method.invoke(cordova, permission);
            hasPermission = bool.booleanValue();
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Cordova v" + CordovaWebView.CORDOVA_VERSION + " does not support runtime permissions so defaulting to GRANTED for " + permission);
        }
        return hasPermission;
    }

    protected boolean shouldShowRequestPermissionRationale(Activity activity, String permission) throws Exception {
        boolean shouldShow;
        try {
            java.lang.reflect.Method method = ActivityCompat.class.getMethod("shouldShowRequestPermissionRationale", Activity.class, java.lang.String.class);
            Boolean bool = (Boolean) method.invoke(null, activity, permission);
            shouldShow = bool.booleanValue();
        } catch (NoSuchMethodException e) {
            throw new Exception("shouldShowRequestPermissionRationale() method not found in ActivityCompat class. Check you have Android Support Library v23+ installed");
        }
        return shouldShow;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        String sRequestId = String.valueOf(requestCode);
        Log.v(TAG, "Received result for permissions request id=" + sRequestId);
        try {
            for (int i = 0, len = permissions.length; i < len; i++) {
                String androidPermission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    boolean showRationale = shouldShowRequestPermissionRationale(this.cordova.getActivity(), androidPermission);
                    if (!showRationale) {

                        if (androidPermission.equals(Manifest.permission.CAMERA)) {
                            _callbackContext.error("Camera Permission not allowed!");
                        } else if (androidPermission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            _callbackContext.error("Storage Permission not allowed!");
                        }
                    } else {
                        if (androidPermission.equals(Manifest.permission.CAMERA)) {
                            _callbackContext.error("Camera Permission not allowed!");
                        } else if (androidPermission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            _callbackContext.error("Storage Permission not allowed!");
                        }
                    }
                } else {
                    if (androidPermission.equals(Manifest.permission.CAMERA)) {
                        takePicture();
                    } else if (androidPermission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        openPhotoLibrary();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred onRequestPermissionsResult: ".concat(e.getMessage()));
            _callbackContext.error("Exception occurred onRequestPermissionsResult: ".concat(e.getMessage()));
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode == RESULT_OK){
            if (requestCode == TAKE_PICTURE_REQUEST_CODE) {
                try{
                    Activity act = this.cordova.getActivity();
                    String packagename = act.getComponentName().getPackageName();
                    Uri imageUri = FileProvider.getUriForFile(
                            act,
                            packagename, //(use your app signature + ".provider" )
                            tempImage);
                    final Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(cordova.getActivity().getContentResolver(),imageUri);

                    //call text identification, label recognition, face detection
                    callML(imageBitmap);

                    return;
                }catch (IOException e){
                    _callbackContext.error("Could not retrieve image!");
                    return;
                }
            }else if (requestCode == OPEN_GALLERY_REQUEST_CODE){
                Uri selectedImage = data.getData();
                try{
                    final Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(cordova.getActivity().getContentResolver(),selectedImage);
                    //call text identification, label recognition, face detection
                    callML(imageBitmap);


                    return;
                }catch (IOException e){
                    _callbackContext.error("Could not retrieve image!");
                    return;
                }
            }
        }
        _callbackContext.error("No Image Selected!");
    }


    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    //                                 Utilities                                  //
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////

    private void callML(Bitmap image){
        switch (myAction){
            case GETTEXT:
                cordova.getThreadPool().execute(() -> runTextRecognition(_callbackContext, image, options.optString("language",""), options.optBoolean("Cloud",false)));
                break;
            case GETLABLE:
                cordova.getThreadPool().execute(() -> runLabelRecognition(_callbackContext, image,options.optBoolean("Cloud",false)));
                break;
            case GETFACE:
                if (options.optBoolean("Tracking",false)){
                    final FirebaseVisionFaceDetectorOptions faceOptions =
                            new FirebaseVisionFaceDetectorOptions.Builder()
                                    .setPerformanceMode(options.optInt("Performance",FirebaseVisionFaceDetectorOptions.ACCURATE))
                                    .setLandmarkMode(options.optInt("Landmarks",FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS))
                                    .setClassificationMode(options.optInt("Classification",FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS))
                                    .setContourMode(options.optInt("Contours",FirebaseVisionFaceDetectorOptions.NO_CONTOURS))
                                    .setMinFaceSize((float)options.optDouble("MinFaceSize",0.1))
                                    .enableTracking()
                                    .build();
                    runFaceDetector(_callbackContext,image,faceOptions);
                }else{
                    final FirebaseVisionFaceDetectorOptions faceOptions =
                            new FirebaseVisionFaceDetectorOptions.Builder()
                                    .setPerformanceMode(options.optInt("Performance",FirebaseVisionFaceDetectorOptions.ACCURATE))
                                    .setLandmarkMode(options.optInt("Landmarks",FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS))
                                    .setClassificationMode(options.optInt("Classification",FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS))
                                    .setContourMode(options.optInt("Contours",FirebaseVisionFaceDetectorOptions.NO_CONTOURS))
                                    .setMinFaceSize((float)options.optDouble("MinFaceSize",0.1))
                                    .build();
                    runFaceDetector(_callbackContext,image,faceOptions);
                }
                break;
        }
    }


    private JSONObject faceLandmarkConvertToJson(FirebaseVisionFaceLandmark landmark) throws JSONException {
        JSONObject landmarkJson = new JSONObject();
        landmarkJson.put(String.valueOf(landmark.getLandmarkType()),visionPointToJson(landmark.getPosition()));
        if (landmarkJson.get(String.valueOf(landmark.getLandmarkType())).equals(new JSONObject())){
            return null;
        }else{
            return landmarkJson;
        }
    }

    private JSONObject faceContourConvertToJson(FirebaseVisionFaceContour contour) throws JSONException {
        JSONObject contourJson = new JSONObject();
        contourJson.put(String.valueOf(contour.getFaceContourType()),visionPointsToJson(contour.getPoints()));
        if (contourJson.get(String.valueOf(contour.getFaceContourType())).equals(new JSONArray())){
            return null;
        }else{
            return contourJson;
        }
    }

    private JSONArray visionPointsToJson(List<FirebaseVisionPoint> positions) throws JSONException {
        JSONArray pointsJson = new JSONArray();
        for (FirebaseVisionPoint position : positions){
            pointsJson.put(visionPointToJson(position));
        }
        return pointsJson;
    }

    private JSONObject visionPointToJson(FirebaseVisionPoint position) throws JSONException {

        JSONObject pointJson =  new JSONObject();
        pointJson.put("x", position.getX());
        pointJson.put("y", position.getY());
        pointJson.put("z", position.getZ());
        return pointJson;
    }


    private JSONObject rectToJson(Rect rect) throws JSONException {
        JSONObject oBloundingBox = new JSONObject();
        oBloundingBox.put("left", rect.left);
        oBloundingBox.put("right", rect.right);
        oBloundingBox.put("top", rect.top);
        oBloundingBox.put("bottom", rect.bottom);
        return oBloundingBox;
    }

    private JSONArray pointsToJson(Point[] points) throws JSONException {
        JSONArray aPoints = new JSONArray();
        for (Point point: points) {
            aPoints.put(pointToJson(point));
        }
        return aPoints;
    }

    private JSONObject pointToJson(Point point) throws JSONException {
        JSONObject pointJson =  new JSONObject();
        pointJson.put("x", point.x);
        pointJson.put("y", point.y);
        return pointJson;
    }
}


