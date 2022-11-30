# Cordova ML Kit Plugin

Implements ML Kit as Cordova plugin on iOS and Android.

# Requirements

Mabs 6

only tested in cordova 7.1.0

## Installation

add a zip called google-services.zip with firebase's google-services(.plist & .json) in "$cordovaprojectfolder/www/google-services/google-services.zip"

``cordova plugin add https://github.com/luisbouca/cordova-ml-kit.git``
or
``<dependency name="cordova-ml-kit url="https://github.com/luisbouca/cordova-ml-kit.git"/>``

## Features

| ML Kit Feature          | Android | Android (Cloud) | iOS | iOS (Cloud) |
|-------------------------|---------|-----------------|-----|-------------|
| Text recognition        | [x]     | [x]             | [x] | [x]         |
| Face detection          | [x]     |                 | [x] |             |
| Barcode scanning        | [ ]     |                 | [ ] |             |
| Image labeling          | [x]     | [x]             | [x] | [x]         |
| Landmark recognition    |         | [ ]             |     | [ ]         |
| Language identification | [x]     |                 | [x] |             |
| Translation             | [ ]     |                 | [ ] |             |
| Smart Reply             | [x]     |                 | [x] |             |
| Custom model inference  | [ ]     |                 | [ ] |             |

| Feature                 | Android | iOS |
|-------------------------|---------|-----|
| Support Video Feed      | [ ]     | [ ] |

Some features of ML Kit are only available on device others only on cloud. Please see https://firebase.google.com/docs/ml-kit/ for more information!

## API Methods
### Text recognition

##### **`getText(success, error, takePhoto, onCloud, language): void`**
Text recognition

| parameter   | type                             | description                 |
| ----------- |----------------------------------|-----------------------------|
| `success`   | `(message: ObjectSuccess)=>void` | Success callback            |
| `error`     | `(message: ObjectError)=>void`   | Error callback              |
| `options`   | `OptionsJson`                    | JSONObject                  |

**`OptionsJson`**

| name        | type         | optional(default) |  description                                                                                                                      |
| ------------|--------------|-------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `TakePhoto` | `Bool`       | [x]`(false)`      | indicates if it should start camera to take a photo or open photo gallery                                                         |
| `Cloud`     | `Bool`       | [x]`(false)`      | Indicates if it should use the cloud service from google. Much better results, but you need an active paid plan (Blaze Plan) and activate it on Google Cloud |
| `Language`  | `string`     | [x]               | [Language Code](https://firebase.google.com/docs/ml-kit/android/recognize-text#1-run-the-text-recognizer), is only used on Cloud  |

###### **`Objects`**

**`ObjectSuccess`**

| name        | type    |  description                                                                   |
| ------------|---------|--------------------------------------------------------------------------------|
| `text`      | `string`| Complete text                                                                  |
| `textBlocks`| `Array` | Text Blocks ([Model](https://firebase.google.com/docs/ml-kit/recognize-text))  |

**`ObjectError`**

| name        | type    |  description           |
| ------------|---------|------------------------|
| `message`   | `string`| error message          |

### Image labeling

##### **`getLabel(success, error, takePhoto, onCloud): void`**
Label recognition

| parameter   | type                             | description                 |
| ----------- |----------------------------------|-----------------------------|
| `success`   | `(message: ArraySuccess)=>void`  | Success callback            |
| `error`     | `(message: ObjectError)=>void`   | Error callback              |
| `options`   | `OptionsJson`                    | JSONObject                  |

###### **`Objects`**

**`OptionsJson`**

| name        | type         | optional(default) |  description                                                             |
| ------------|--------------|-------------------|--------------------------------------------------------------------------|
| `TakePhoto` | `Bool`       | [x]`(false)`      | indicates if it should start camera to take a photo or open photo gallery|
| `Cloud`     | `Bool`       | [x]`(false)`      | Indicates if it should use the cloud service from google. Much better results, but you need an active paid plan (Blaze Plan) and activate it on Google Cloud |

**`ArraySuccess`**

| name        | type    |  description                                                                   |
| ------------|---------|--------------------------------------------------------------------------------|
| `label`     | `string`| label's text description                                                       |
| `confidence`| `string`| confidence score of the match                                                  |
| `entityId`  | `string`| ([Knowledge Graph entity ID ](https://developers.google.com/knowledge-graph/)) |

**`ObjectError`**

| name        | type    |  description           |
| ------------|---------|------------------------|
| `message`   | `string`| error message          |


### Face detection

##### **`getFace(success, error, takePhoto, options): void`**
Label recognition

| parameter   | type                             | description      |
| ----------- |----------------------------------|------------------|
| `success`   | `(message: ArraySuccess)=>void`  | Success callback |
| `error`     | `(message: ObjectError)=>void`   | Error callback   |
| `options`   | `OptionsJson`                    | JSONObject       |

###### **`Objects`**

**([OptionsJson](https://firebase.google.com/docs/ml-kit/android/detect-faces#1.-configure-the-face-detector))**

Note that when contour detection is enabled, only one face is detected, so face tracking doesn't produce useful results. For this reason, and to improve detection speed, don't enable both contour detection and face tracking.

| name             | type    | optional(default) | description                                                                                   |
| -----------------|---------|-------------------|-----------------------------------------------------------------------------------------------|
| `TakePhoto`      | `Bool`  | [x]`(false)`      | indicates if it should start camera to take a photo or open photo gallery                     |
| `Tracking`       | `Bool`  | [x]`(false)`      | Whether or not to assign faces an ID, which can be used to track faces across images          |
| `Performance`    | `int`   | [x]`(2)`          | Favor speed or accuracy when detecting faces                                                  |
| `Landmarks`      | `int`   | [x]`(2)`          | Whether to attempt to identify facial "landmarks": eyes, ears, nose, cheeks, mouth, and so on |
| `Classification` | `int`   | [x]`(2)`          | Whether or not to classify faces into categories such as "smiling", and "eyes open"           |
| `Contours`       | `int`   | [x]`(1)`          | Whether to detect the contours of facial features. Contours are detected for only the most prominent face in an image |
| `MinFaceSize`    | `float` | [x]`(0.1)`        | The minimum size, relative to the image, of faces to detect                                   |

for int's:
    1 = false
    2 = true



**`ArraySuccess`**

| name             | type    |  description                                                                              |
| -----------------|---------|-------------------------------------------------------------------------------------------|
| `bounds`         | `Rect`  | A face's bounds                                                                           |
| `rotY`           | `float` | A face with a positive Euler Y angle is turned to the camera's right and to its left.     |
| `rotZ`           | `float` | A face with a positive Euler Z angle is rotated counter-clockwise relative to the camera. |
| `Landmarks`      | `object`| A point of interest within a face.                                                        |
| `Contours`       | `object`| A set of points that represent the shape of a facial feature.                             |
| `Classification` | `object`| Determines whether a certain facial characteristic is present                             |
| `TrackingId`     | `int`   |Eextends face detection to video sequences                                                 |

([More Detailed Information](https://firebase.google.com/docs/ml-kit/face-detection-concepts))

**`Rect`**

| name         | type    | 
| -------------|---------|
| `top`        | `float` |
| `bottom`     | `float` |
| `right`      | `float` |
| `left`       | `float` |

**`Point`**

| name         | type    |
| -------------|---------|
| `x`          | `float` |
| `y`          | `float` |
| `z`          | `float` |

**`Landmarks`**

| name         | type    |
| -------------|---------|
| `LEFT_CHEEK` | `Point` |
| `LEFT_EAR`   | `Point` |
| `LEFT_EYE`   | `Point` |
| `MOUTH_LEFT` | `Point` |
| `NOSE_BASE`  | `Point` |
| `RIGHT_CHEEK`| `Point` |
| `RIGHT_EAR`  | `Point` |
| `RIGHT_EYE`  | `Point` |
| `MOUTH_RIGHT`| `Point` |

**`Contours`**

| name                   | type          |
| -----------------------|---------------|
| `ALL_POINTS`           | `Point Array` |
| `FACE`                 | `Point Array` |
| `LEFT_EYEBROW_TOP`     | `Point Array` |
| `LEFT_EYEBROW_BOTTOM`  | `Point Array` |
| `RIGHT_EYEBROW_TOP`    | `Point Array` |
| `RIGHT_EYEBROW_BOTTOM` | `Point Array` |
| `LEFT_EYE`             | `Point Array` |
| `RIGHT_EYE`            | `Point Array` |
| `UPPER_LIP_TOP`        | `Point Array` |
| `UPPER_LIP_BOTTOM`     | `Point Array` |
| `LOWER_LIP_TOP`        | `Point Array` |
| `LOWER_LIP_BOTTOM`     | `Point Array` |
| `NOSE_BRIDGE`          | `Point Array` |
| `NOSE_BOTTOM`          | `Point Array` |


**`Classification`**

| name                      | type    |
| --------------------------|---------|
| `SmileProbability`        | `float` |
| `LeftEyeOpenProbability`  | `float` |
| `RightEyeOpenProbability` | `float` |


**`ObjectError`**

| name        | type    |  description           |
| ------------|---------|------------------------|
| `message`   | `string`| error message          |


### Language identification

##### **`identifyLang(success, error,options): void`**
Identify Language Scanning

| parameter   | type                             | description                 |
| ----------- |----------------------------------|-----------------------------|
| `success`   | `(message: ArraySuccess)=>void`  | Success callback            |
| `error`     | `(message: ObjectError)=>void`   | Error callback              |
| `options`   | `OptionsJson`                    | JSONObject                  |

**`OptionsJson`**

| name             | type     | optional(default) | description                        |
| -----------------|----------|-------------------|------------------------------------|
| `text`           | `String` | [ ]               | String to identify the language of |
| `confidence`     | `float`  | [x]`(0.5)`        | minimum confidence                 |


**`ArraySuccess`**

| name         | type     |  description                          |
| -------------|----------|---------------------------------------|
| `code`       | `string` | Code of the language identified       |
| `confidence` | `float`  | Confidence of the language identified |

###### **`ObjectError`**

| name        | type    |  description           |
| ------------|---------|------------------------|
| `message`   | `string`| error message          |


### Smart Reply

To create a smart Reply first you need to add the conversation so that the model can use that information to create the reply.

##### **`reply(success, error): void`**
Suggest replies to a given conversation

| parameter   | type                                 | description                 |
| ----------- |--------------------------------------|-----------------------------|
| `success`   | `(message: ArraySuccessReply)=>void` | Success callback            |
| `error`     | `(message: ObjectError)=>void`       | Error callback              |
| `options`   | `OptionsJsonReply`                   | JSONObject                  |


**`OptionsJsonReply`**

| name         | type    |  description                            | optional(Default) |
| -------------|---------|-----------------------------------------|-------------------|
| `Identifier` | `String`| Conversation Identifier                 | [ ]               |

**`ArraySuccessReply`**

| name         | type     |  description                          |
| -------------|----------|---------------------------------------|
| `Reply`      | `string` | Reply possibility to the conversation |


##### **`addMessage(success, error): void`**
Add Message to a given conversation or create one if none matches the identification string

| parameter   | type                              | description                 |
| ----------- |-----------------------------------|-----------------------------|
| `success`   | `(message: Array(Message))=>void` | Success callback            |
| `error`     | `(message: ObjectError)=>void`    | Error callback              |
| `options`   | `OptionsJsonAdd`                  | JSONObject                  |


**`OptionsJsonAdd`**

| name             | type             | optional(default) | description                                |
| -----------------|------------------|-------------------|--------------------------------------------|
| `Identifier`     | `String`         | [ ]               | String to use to identify the conversation |
| `Messages`       | `Array(Message)` | [ ]               | messages to add                            |


**`Message`**

| name                | type     | optional(default) | description                                                 |
| --------------------|----------|-------------------|-------------------------------------------------------------|
| `message`           | `String` | [ ]               | Message text                                                |
| `timestamp`         | `Double` | [ ]               | Timestamp when the message was sent                         |
| `personId`          | `String` | [x]               | Id of who sent the message(do not use if it's your message) |

##### **`removeMessage(success, error): void`**
Remove message from a given conversation

| parameter   | type                             | description                 |
| ----------- |----------------------------------|-----------------------------|
| `success`   | `(message: Message)=>void`       | Success callback            |
| `error`     | `(message: ObjectError)=>void`   | Error callback              |
| `options`   | `OptionsJsonRemove`              | JSONObject                  |



**`OptionsJsonRemove`**

| parameter    | type     | description                                |
| -------------|----------|--------------------------------------------|
| `Identifier` | `String` | String to use to identify the conversation |
| `Id`         | `int`    | Id of the message                          |


### Custom Model Inference

### Barcode scanning

### Landmark recognition

### Translation