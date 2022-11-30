/********* MlKitPlugin.m Cordova Plugin Implementation *******/

import Foundation

import WebKit

import AVFoundation

import MobileCoreServices

import FirebaseMLVision

import FirebaseMLCommon

import FirebaseMLNaturalLanguage

import FirebaseMLNLLanguageID
import FirebaseMLNLSmartReply

var _command: CDVInvokedUrlCommand!

@objc(MlKitPlugin) class MlKitPlugin : CDVPlugin,UIImagePickerControllerDelegate,UINavigationControllerDelegate{
    
    static let LOG_TAG = "MLKitPlugin";
    
    var action: Actions?
    
    var options:[String:Any]?

    var vision:Vision?

    enum Actions {
        case INVALID
        case GETTEXT
        case GETLABLE
        case GETFACE
        case IDENTIFYLANG
        case REPLY
        case ADDMESSAGE
        case REMOVEMESSAGE
        
        static let allValues = [INVALID,GETTEXT,GETLABLE,GETFACE,IDENTIFYLANG,REPLY,ADDMESSAGE,REMOVEMESSAGE]
        
    }

    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    //                                Text Recognition                              //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //


    @objc(getText:)
    func getText(command: CDVInvokedUrlCommand){
        action = Actions.GETTEXT
        _command=command
        
        options = (command.argument(at: 0, withDefault:[:]) as! [String:Any])
        vision = Vision.vision()
        if options!["TakePicture"] as? Bool ?? false {
            commandDelegate.run(inBackground: {
                self.useCamera()
            })
        }else{
            commandDelegate.run(inBackground: {
                self.useCameraRoll()
            })
        }
    }

    func runTextRecognition(for image:VisionImage,onCloud cloud:Bool,in lang:String,call command:CDVInvokedUrlCommand) {
        
        var textRecognizer:VisionTextRecognizer;
        if cloud {
            if lang.count > 0 && lang != "" {
                let options = VisionCloudTextRecognizerOptions()
                options.languageHints = [lang]
                textRecognizer = vision?.cloudTextRecognizer(options: options) ?? Vision.vision().cloudTextRecognizer(options: options)
            }else{
                textRecognizer = vision?.cloudTextRecognizer() ?? Vision.vision().cloudTextRecognizer()
            }
        }else{
            textRecognizer = vision?.onDeviceTextRecognizer() ?? Vision.vision().onDeviceTextRecognizer()
        }
        textRecognizer.process(image) {result,error in
            guard error == nil, let result:VisionText = result else{
                let errorString = error?.localizedDescription ?? "No Result"
                self.sendPluginError(message: errorString, call: command)
                return
            }
            var json = [String:Any]()
            var blocks:[[String:Any]] = Array(repeating: ["test":"test"], count: result.blocks.count)
            
            var idxBlock = 0
            for block:VisionTextBlock in result.blocks {
                self.logDebug(message: block.text)
                
                var oBlock = [String:Any]()
                var lines:[[String:Any]] = Array(repeating: ["test":"test"], count: block.lines.count)
                var idxLine = 0
                
                for line:VisionTextLine in block.lines {
                    var oLine = [String:Any]()
                    oLine["text"] = line.text
                    oLine["confidence"] = line.confidence
                    oLine["boundingBox"] = self.rectToJson(for: line.frame)
                    oLine["cornerPoints"] = self.pointsToJson(for: line.cornerPoints ?? [NSValue]())
                    lines[idxLine] = oLine
                    idxLine = idxLine + 1
                }
                
                oBlock["text"] = block.text
                oBlock["confidence"] = block.confidence
                oBlock["boundingBox"] = self.rectToJson(for: block.frame)
                oBlock["cornerPoints"] = self.pointsToJson(for: block.cornerPoints ?? [NSValue]())
                oBlock["lines"] = block.text
                blocks[idxBlock] = oBlock
                idxBlock = idxBlock + 1
            }
            json["text"] = result.text
            json["textBlocks"] = blocks
            
            self.sendPluginResult(message: json, call: command)
        }
    }

    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    //                              Label Identification                            //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    
    @objc(getLabel:)
    func getLabel(command: CDVInvokedUrlCommand){
        action = Actions.GETLABLE
        _command=command
        
        options = (command.argument(at: 0, withDefault:[:]) as! [String:Any])
        vision = Vision.vision()
        if options!["TakePicture"] as? Bool ?? false {
            commandDelegate.run(inBackground: {
                self.useCamera()
            })
        }else{
            commandDelegate.run(inBackground: {
                self.useCameraRoll()
            })
        }
    }

    func runLabelIdentifier(for image:VisionImage,onCloud cloud:Bool,call command:CDVInvokedUrlCommand){
        if cloud {
            let labelDetector = vision?.cloudImageLabeler() ?? Vision.vision().cloudImageLabeler()
            labelDetector.process(image, completion: lableResponseHandler)
        }else{
            let labelDetector = vision?.onDeviceImageLabeler() ?? Vision.vision().onDeviceImageLabeler()
            labelDetector.process(image, completion: lableResponseHandler)
        }
        
    }
    
    func lableResponseHandler(result:[VisionImageLabel]?,error:Error?){
        guard error == nil, let result:[VisionImageLabel] = result else{
            let errorString = error?.localizedDescription ?? "No Result"
            self.sendPluginError(message: errorString, call: _command)
            return
        }
        var json:[[String:Any]] = Array(repeating: ["test":"test"], count: result.count)
        var idxLabel = 0
        for label in result {
            self.logDebug(message: label.text)
            
            var oLabel = [String:Any]()
            
            oLabel["label"] = label.text
            oLabel["confidence"] = label.confidence
            oLabel["entityID"] = label.entityID
            
            json[idxLabel] = oLabel
            idxLabel = idxLabel + 1
        }
        
        self.sendPluginResult(message: json, call: _command)
    }
    
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    //                               Face Detection                                 //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //

    @objc(getFace:)
    func getFace(command: CDVInvokedUrlCommand){
        action = Actions.GETFACE
        _command=command
        
        options = (command.argument(at: 0, withDefault:[:]) as! [String:Any])
        
        vision = Vision.vision()
        if options!["TakePicture"] as? Bool ?? false {
            commandDelegate.run(inBackground: {
                self.useCamera()
            })
        }else{
            commandDelegate.run(inBackground: {
                self.useCameraRoll()
            })
        }
    }

    func runFaceDetection(for image:VisionImage,with options:VisionFaceDetectorOptions,call command:CDVInvokedUrlCommand) {
        
        let faceDetector:VisionFaceDetector = vision?.faceDetector(options: options) ?? Vision.vision().faceDetector(options: options);
        
        faceDetector.process(image, completion: {result, error in
            
            guard error == nil, let result:[VisionFace] = result else{
                let errorString = error?.localizedDescription ?? "No Result"
                self.sendPluginError(message: errorString, call: command)
                return
            }
            
            var json:[[String:Any]] = Array(repeating:["test":"test"],count: 0)
            
            var idxFace = 0
            for face in result{
                var faceJson = [String:Any]()
                
                let bounds = face.frame
                
                faceJson["Bounds"] = self.rectToJson(for: bounds)
                
                // Head is rotated to the right rotY degrees
                if face.hasHeadEulerAngleY {
                    faceJson["RotY"] = face.headEulerAngleY
                }
                
                // Head is tilted sideways rotZ degrees
                if face.hasHeadEulerAngleZ {
                    faceJson["RotZ"] = face.headEulerAngleZ
                }
                
                // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                // nose available):
                if options.landmarkMode == VisionFaceDetectorLandmarkMode.all {
                    
                    var landmarks:[[String:Any]] = Array(repeating: ["test":"test"], count: 0)
                    
                    let landmarksValues = [FaceLandmarkType.mouthBottom
                        , FaceLandmarkType.leftCheek
                        , FaceLandmarkType.leftEar
                        , FaceLandmarkType.leftEye
                        , FaceLandmarkType.mouthLeft
                        , FaceLandmarkType.noseBase
                        , FaceLandmarkType.rightCheek
                        , FaceLandmarkType.rightEar
                        , FaceLandmarkType.rightEye
                        , FaceLandmarkType.mouthRight]
                    for landmarkValue in landmarksValues {
                        let landmarkOptional = face.landmark(ofType: landmarkValue)
                        if let landmark:VisionFaceLandmark = landmarkOptional{
                            
                            let landmarkJson:[String:Any] = self.landmarkToJson(for: landmark)
                            landmarks.append(landmarkJson)
                        }
                    }
                    if landmarks.count>0 {
                        faceJson["Landmarks"] = landmarks
                    }
                    
                }
                
                // If contour detection was enabled:
                if options.contourMode == VisionFaceDetectorContourMode.all {
                    var contours:[[String:Any]] = Array(repeating: ["test":"test"], count: 0)
                    
                    let contoursValues = [FaceContourType.all
                        , FaceContourType.face
                        , FaceContourType.leftEyebrowTop
                        , FaceContourType.leftEyebrowBottom
                        , FaceContourType.rightEyebrowTop
                        , FaceContourType.rightEyebrowBottom
                        , FaceContourType.leftEye
                        , FaceContourType.rightEye
                        , FaceContourType.upperLipTop
                        , FaceContourType.upperLipBottom
                        , FaceContourType.lowerLipTop
                        , FaceContourType.lowerLipBottom
                        , FaceContourType.noseBridge
                        , FaceContourType.noseBottom]
                    for contourValue in contoursValues {
                        let contourOptional = face.contour(ofType: contourValue)
                        if let contour:VisionFaceContour = contourOptional{
                            
                            let contourJson:[String:Any] = self.contourToJson(for: contour)
                            contours.append(contourJson)
                        }
                    }
                    if contours.count>0 {
                        faceJson["Contours"] = contours
                    }
                }
                
                // If classification was enabled:
                if options.classificationMode == VisionFaceDetectorClassificationMode.all {
                    var classification = [String:Any]()
                    if face.hasSmilingProbability {
                        classification["SmileProbability"] = face.smilingProbability
                    }
                    
                    if face.hasLeftEyeOpenProbability {
                        classification["LeftEyeOpenProbability"] = face.leftEyeOpenProbability
                    }
                    
                    if face.hasRightEyeOpenProbability {
                        classification["RightEyeOpenProbability"] = face.rightEyeOpenProbability
                    }
                    faceJson["Classification"] = classification
                }
                
                // If face tracking was enabled:
                if options.isTrackingEnabled {
                    faceJson["TrackingId"] = face.trackingID
                }
                
                
                if faceJson.count > 0 {
                    json.append(faceJson)
                    idxFace = idxFace + 1
                }
            }
            
            if json.count > 0{
                self.sendPluginResult(message: json, call: command)
            }else{
                self.sendPluginError(message: "No Result", call: command)
            }
            
            
        })
    }

    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    //                                Smart Reply                                   //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //

    var conversations:[String:[TextMessage]]?

    @objc(addMessage:)
    func addMessage(command: CDVInvokedUrlCommand){
        //options = [String:Any]()
        action = Actions.ADDMESSAGE
        _command=command
        
        options = (command.argument(at: 0, withDefault:["no":"no"]) as! [String:Any])
        
        conversations = [String:[TextMessage]]()
        
        commandDelegate.run(inBackground: {
            var identifier = ""
            var textMessages:[TextMessage] = [TextMessage]()
            self.options!.contains(where: { (key,value) in
                if key.elementsEqual("Identifier"){
                    identifier = value as! String
                }else if key.elementsEqual("Messages"){
                    let messages:[[String:Any]] = value as! Array
                    for message in messages {
                        var text = ""
                        var timestamp = 0.0
                        var id:String?
                        message.contains(where: { (key,value) in
                            if key.elementsEqual("message"){
                                text = value as! String
                            }else if key.elementsEqual("timestamp"){
                                timestamp = value as! Double
                            }else if key.elementsEqual("personId"){
                                id = value as? String
                            }
                            return false
                        })
                        
                        let local = !(id?.isEmpty ?? true)
                        
                        let textMessage = TextMessage(text: text, timestamp: timestamp, userID: id ?? "LocalId", isLocalUser: local)
                        textMessages.append(textMessage)
                    }
                    
                }
                return false
            })
            if !identifier.isEmpty && textMessages.count>0{
                if self.conversations == nil {
                    self.conversations = [String:[TextMessage]]()
                }
                self.conversations![identifier] = textMessages
                self.sendPluginResult(message: self.convertToJson(in: self.conversations![identifier]!), call: command)
                return
            }
            self.sendPluginError(message: "Unexpected Error Occured!", call: command)
        })
    }
    
    @objc(removeMessage:)
    func removeMessage(command: CDVInvokedUrlCommand){
        //options = [String:Any]()
        action = Actions.REMOVEMESSAGE
        _command=command
        
        options = (command.argument(at: 0, withDefault:["no":"no"]) as! [String:Any])
        
        commandDelegate.run(inBackground: {
            var identifier:String?
            var id:Int?
            self.options!.contains(where: { (key,value) in
                if key.elementsEqual("Identifier"){
                    identifier = value as? String
                }else if key.elementsEqual("Id"){
                    id = value as? Int
                }
                return false
            })
            
            if (identifier != nil) && (id != nil) && (self.conversations != nil) {
                if self.conversations!.contains(where: { (key,value) in
                    if key.elementsEqual(identifier!){
                        return true
                    }
                    return false}) {
                    if self.conversations![identifier!]!.count > id! {
                        self.conversations![identifier!]?.remove(at: id!)
                        self.sendPluginResult(message: self.convertToJson(in: self.conversations![identifier!]!), call: command)
                        return
                    }else{
                        self.sendPluginError(message: "Id does not Exist", call: command)
                    }
                }
            }else{
                self.sendPluginError(message: "Identifier does not Exist", call: command)
            }
            self.sendPluginError(message: "Unexpected Error Occured!", call: command)
        })
    }
    
    @objc(reply:)
    func reply(command: CDVInvokedUrlCommand){
        //options = [String:Any]()
        action = Actions.REPLY
        _command=command
        
        options = (command.argument(at: 0, withDefault:["no":"no"]) as! [String:Any])
        
        if self.options!.contains(where: { (key,value) in
            if key.elementsEqual("Identifier"){
                return true
            }
            return false
        }){
            let identifier = options!["Identifier"] as? String ?? ""
            if conversations?.contains(where: { (key,value) in
                if key.elementsEqual(identifier){
                    return true
                }
                return false
            }) ?? false{
                reply(to:conversations![identifier]!,call: command)
            }else{
                sendPluginError(message: "Conversation doesn't exist!", call: command)
            }
        }else{
            sendPluginError(message: "Options Identifier propriety not found!", call: command)
        }
        self.sendPluginError(message: "Unexpected Error Occured!", call: command)
        
    }

    func reply(to conversation:[TextMessage], call command:CDVInvokedUrlCommand){
        let smartReplier = NaturalLanguage.naturalLanguage().smartReply()
        
        smartReplier.suggestReplies(for: conversation) { result, error in
            guard error == nil, let results = result else {
                let errorString = error?.localizedDescription ?? "No Result"
                self.sendPluginError(message: errorString, call: command)
                return
            }
            if (results.status == .notSupportedLanguage) {
                self.sendPluginError(message: "No Result", call: command)
                return
            } else if (results.status == .success) {
                var json:[[String:Any]] = Array(repeating: ["test":"test"], count: 0)
                for reply in results.suggestions{
                    var replyJson = [String:Any]()
                    replyJson["Reply"] = reply.text
                    json.append(replyJson)
                    
                }
                self.sendPluginResult(message: json, call: command)
            }
        }
    }

    func convertToJson(in json:[TextMessage])->String{
        var finalString = "["
        var notFirst = false
        for item in json {
            if notFirst {
                finalString += ","
            }else{
                notFirst = true
            }
            finalString += convertToJson(in :textMessageToJson(for: item))
        }
        finalString += "]"
        return finalString
    }
    
    func textMessageToJson(for message: TextMessage) -> [String:Any] {
        var aMessage = [String:Any]()
        aMessage["text"] = message.text
        aMessage["timestamp"] = String.init(message.timestamp)
        
        return aMessage
    }
    
    
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    //                              Language Identifier                             //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //

    @objc(identifyLang:)
    func identifyLang(command: CDVInvokedUrlCommand){
        action = Actions.IDENTIFYLANG
        _command=command
        
        options = (command.argument(at: 0, withDefault:["no":"no"]) as! [String:Any])
        
        commandDelegate.run(inBackground: {
            let text = self.options?["text"] as? String ?? ""
            if text.count > 0 {
                let confidence = self.options?["confidence"] as? Float ?? 0.5
                let langIdOptions = LanguageIdentificationOptions(confidenceThreshold: confidence)
                self.runLanguageIdentifier(for: text,with: langIdOptions,call: command)
            }else{
                self.sendPluginError(message: "No Text was sent!", call: command)
            }
        })
    }

    func runLanguageIdentifier(for text:String,with options: LanguageIdentificationOptions, call command:CDVInvokedUrlCommand){
        let languageId = NaturalLanguage.naturalLanguage().languageIdentification(options: options)
        languageId.identifyPossibleLanguages(for: text, completion: {(results, error) in
            guard error == nil, let results:[IdentifiedLanguage] = results else{
                let errorString = error?.localizedDescription ?? "No Result"
                self.sendPluginError(message: errorString, call: command)
                return
            }
            
            var json:[[String:Any]] = Array(repeating: ["test":"test"], count: 0)
            for language in results{
                var languageJson = [String:Any]()
                if language.languageCode.elementsEqual("und") {
                    self.sendPluginError(message: "No Result", call: command)
                    return
                }else{
                    languageJson["Code"] = language.languageCode
                    languageJson["Confidence"] = String.init(language.confidence)
                    json.append(languageJson)
                }
            }
            self.sendPluginResult(message: json, call: command)
            
        })
    }


    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    //                                   Camera                                     //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //

    func useCamera() {
        
        if UIImagePickerController.isSourceTypeAvailable(
            UIImagePickerControllerSourceType.camera) {
            DispatchQueue.main.async {
                let imagePicker = UIImagePickerController()
                
                imagePicker.delegate = self
                imagePicker.sourceType =
                    UIImagePickerControllerSourceType.camera
                imagePicker.mediaTypes = [kUTTypeImage as String]
                imagePicker.allowsEditing = false
                
                self.viewController.present(imagePicker, animated: true,
                                            completion: {
                                                imagePicker.delegate = self
                })
            }
        }
    }
    
    func useCameraRoll() {
        
        if UIImagePickerController.isSourceTypeAvailable(
            UIImagePickerControllerSourceType.savedPhotosAlbum) {
            
            DispatchQueue.main.async {
                let imagePicker = UIImagePickerController()
                
                imagePicker.delegate = self
                imagePicker.sourceType =
                    UIImagePickerControllerSourceType.photoLibrary
                imagePicker.mediaTypes = [kUTTypeImage as String]
                imagePicker.allowsEditing = false
                
                self.viewController.present(imagePicker, animated: true,
                                            completion: {
                                                imagePicker.delegate = self
                })
            }
        }
    }
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
        let mediaType = info[UIImagePickerControllerMediaType] as! NSString
        
        viewController.dismiss(animated: true, completion: nil)
        
        if mediaType.isEqual(to: kUTTypeImage as String) {
            let image = info[UIImagePickerControllerOriginalImage]
                as! UIImage
            callMlKitImage(for: image)
        }
    }
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        
        let mediaType = info[UIImagePickerControllerMediaType as UIImagePickerController.InfoKey] as! NSString
        let image = info[UIImagePickerControllerOriginalImage as UIImagePickerController.InfoKey]
            as! UIImage
        
        var myInfo = [String : Any]()
        myInfo[UIImagePickerControllerMediaType] = mediaType
        myInfo[UIImagePickerControllerOriginalImage] = image
        imagePickerController(picker, didFinishPickingMediaWithInfo: myInfo)
    }
    
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    //                                  Utilities                                   //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    // //////////////////////////////////////////////////////////////////////////// //
    
    func callMlKitImage(for image:UIImage){
        let visionImage = VisionImage.init(image: image)
        switch action {
        case .GETTEXT:
            runTextRecognition(for: visionImage, onCloud: options!["Cloud"] as? Bool ?? false , in: options!["language"] as? String ?? "", call: _command)
        case .GETLABLE:
            runLabelIdentifier(for: visionImage, onCloud: options!["Cloud"] as? Bool ?? false, call: _command)
        case .GETFACE:
            let visionOptions = VisionFaceDetectorOptions()
            visionOptions.performanceMode = VisionFaceDetectorPerformanceMode.init(rawValue: options?["Performance"] as? UInt ?? 2) ?? VisionFaceDetectorPerformanceMode.accurate
            visionOptions.landmarkMode = VisionFaceDetectorLandmarkMode.init(rawValue: options?["Landmark"] as? UInt ?? 2) ?? VisionFaceDetectorLandmarkMode.all
            visionOptions.classificationMode = VisionFaceDetectorClassificationMode.init(rawValue: options?["Classification"] as? UInt ?? 2) ?? VisionFaceDetectorClassificationMode.all
            visionOptions.contourMode = VisionFaceDetectorContourMode.init(rawValue: options?["Contours"] as? UInt ?? 1) ?? VisionFaceDetectorContourMode.none
            
            visionOptions.minFaceSize = CGFloat(options?["MinFaceSize"] as? Float ?? 0.1)
            
            runFaceDetection(for: visionImage,with: visionOptions, call:_command)
        default:
            sendPluginError(message: "Invalid Action detected after image selection!", call: _command)
        }
    }
    
    func contourToJson (for contour:VisionFaceContour) -> [String:Any]{
        let contourPoints =  pointsToJson(for: contour.points)
        var contourJson = [String:Any]()
        contourJson[contour.type.rawValue] = contourPoints
        return contourJson
    }
    
    func landmarkToJson (for landmark:VisionFaceLandmark) -> [String:Any]{
        let landmarkPoint =  pointToJson(for: landmark.position)
        var landmarkJson = [String:Any]()
        landmarkJson[landmark.type.rawValue] = landmarkPoint
        return landmarkJson
    }
    
    func pointToJson(for point:VisionPoint ) -> [String:Any] {
        var oPoint = [String:Any]()
        oPoint["x"] = point.x
        oPoint["y"] = point.y
        oPoint["z"] = point.z
        return oPoint
    }
    
    func pointsToJson(for points:[VisionPoint] ) -> Array<Any> {
        var aPoints:[[String:Any]] = Array(repeating: ["test":"test"], count: points.count)
        
        for point:VisionPoint in points{
            
            let aPoint = pointToJson(for: point)
            aPoints.append(aPoint)
        }
        return aPoints
    }
    
    func rectToJson(for rec:CGRect ) -> [String:Any] {
        var oBoundBox = [String:Any]()
        oBoundBox["left"] = rec.minX
        oBoundBox["right"] = rec.maxX
        oBoundBox["top"] = rec.maxY
        oBoundBox["bottom"] = rec.minY
        return oBoundBox
    }
    
    func pointsToJson(for points: [NSValue]) -> Array<Any> {
        var aPoints:[[String:Any]] = Array(repeating: ["test":"test"], count: points.count)
        var idxPoint = 0
        
        for point:NSValue in points{
            
            let aPoint = pointToJson(for: point)
            
            aPoints[idxPoint] = aPoint
            idxPoint = idxPoint + 1
        }
        return aPoints
    }
    
    func pointToJson(for point: NSValue) -> [String:Any] {
        var aPoint = [String:Any]()
        aPoint["x"] = point.cgPointValue.x
        aPoint["y"] = point.cgPointValue.y
        
        return aPoint
    }
    
    func convertToJson(in json:[String:Any])->String{
        var finalString = "{"
        var notFirst = false
        for (jsonIdx,jsonItem) in json {
            if notFirst {
                finalString += ","
            }else{
                notFirst = true
            }
            finalString += "\"\(jsonIdx)\" : ";
            let jsonItemArray = jsonItem as? [[String:Any]]
            let jsonItemJson = jsonItem as? [String:Any]
            if jsonItemArray != nil {
                finalString += convertToJson(in: jsonItemArray!)
            }else if jsonItemJson != nil{
                finalString += convertToJson(in: jsonItemJson!)
            }else{
                let item = jsonItem as? String
                if item != nil {
                    finalString += "\"\(item!)\""
                }else{
                    let floatItem = jsonItem as? CGFloat
                    if floatItem != nil {
                        finalString += "\"\(floatItem!)\""
                    }else{
                        finalString += "\"\""
                    }
                    
                }
            }
        }
        finalString += "}"
        return finalString
    }
    
    func convertToJson(in json:[[String:Any]])->String{
        var finalString = "["
        var notFirst = false
        for item in json {
            if notFirst {
                finalString += ","
            }else{
                notFirst = true
            }
            finalString += convertToJson(in: item)
        }
        finalString += "]"
        return finalString
    }
    
    func sendPluginResult(message result:[String:Any],call command:CDVInvokedUrlCommand){
        let json = convertToJson(in: result)
        sendPluginResult(message: json, call: command)
    }
    
    func sendPluginResult(message result:[[String:Any]],call command:CDVInvokedUrlCommand){
        let json = convertToJson(in: result)
        sendPluginResult(message: json, call: command)
    }
    
    func sendPluginResult(message result:String,call command:CDVInvokedUrlCommand) {
        var pluginResult:CDVPluginResult;
        pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    func sendPluginError(message result:String,call command:CDVInvokedUrlCommand) {
        var pluginResult:CDVPluginResult;
        pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: result)
        logError(message: result)
        commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
    
    func handlePluginException(with exception: NSException,call command:CDVInvokedUrlCommand)
    {
        let pluginResult = CDVPluginResult(status:CDVCommandStatus_ERROR, messageAs:exception.reason);
        logError(message :"EXCEPTION: "+(exception.reason ?? "unknown!"));
        commandDelegate.send(pluginResult, callbackId:command.callbackId);
    }
    
    func executeGlobalJavascript(withScript jsString: String) {
        commandDelegate.evalJs(jsString);
    }
    
    func logDebug(message msg:String)
    {
        NSLog("%@: %@", MlKitPlugin.LOG_TAG, msg);
        let jsString = "console.log(\""+MlKitPlugin.LOG_TAG+": "+escapeDoubleQuotes(str: msg)+"\")";
        executeGlobalJavascript(withScript: jsString);
    }
    
    func logError(message msg:String)
    {
        NSLog("%@ ERROR: %@", MlKitPlugin.LOG_TAG, msg);
        let jsString = "console.error(\""+MlKitPlugin.LOG_TAG+": "+escapeDoubleQuotes(str: msg)+"\")";
        executeGlobalJavascript(withScript: jsString);
    }
    
    func escapeDoubleQuotes(str:String) ->String{
        return str.replacingOccurrences(of: "\"", with: "\\\"");
    }
    
}
