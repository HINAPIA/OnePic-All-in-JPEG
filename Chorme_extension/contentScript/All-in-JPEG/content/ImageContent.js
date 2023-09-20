import ContentAttribute from "./contentType.js";
import Picture from "./Picture.js"
export default class ImageContent {
    constructor() {
        this.pictureList = [];
        this.pictureCount = 0;
        this.jpegMetaData = new Uint8Array();
        this.mainPicture = null;
        this.mainBitmap = null;
        this.bitmapList = [];
        this.attributeBitmapList = [];
        this.bitmapListAttribute = null;
        this.checkBitmapList = false;
        this.checkPictureList = false;
        this.checkMain = false;
        this.checkMagicCreated = false;
        this.checkRewind = false;
        this.checkAdded = false;
        this.checkMainChanged = false;
        this.checkEditChanged = false;
        this.isSetBitmapListStart = false;
    }

    init() {
        this.checkBitmapList = false;
        this.checkPictureList = false;
        this.checkMain = false;
        this.setCheckAttribute();
        this.pictureList = [];
        this.pictureCount = 0;
        this.bitmapList = [];
        this.mainBitmap = null;
        this.attributeBitmapList = [];
        this.bitmapListAttribute = null;
        this.isSetBitmapListStart = false;
    }

    setCheckAttribute() {
        this.checkMagicCreated = false;
        this.checkRewind = false;
        this.checkAdded = false;
        this.checkMainChanged = false;
        this.checkEditChanged = false;
    }

    // async setContent(byteArrayList, contentAttribute) {
    //     this.init();
    //     this.jpegMetaData = this.extractJpegMeta(byteArrayList[0], contentAttribute);
    //     for (let i = 0; i < byteArrayList.length; i++) {
    //         let app1Bytes = this.extractAPP1(byteArrayList[i]);
    //         let frameBytes = await this.extractFrame(byteArrayList[i], contentAttribute);
    //         //let picture = new Picture(contentAttribute, app1Bytes, frameBytes);
    //         let picture = new Picture(null, app1Bytes, frameBytes, contentAttribute, 0, null)
    //         picture.waitForByteArrayInitialized();
    //         this.insertPicture(picture);
    //         console.log("AiJPEG", "setImageContnet: picture[" + i + "] 완성");
    //         if (i === 0) {
    //             this.mainPicture = picture;
    //             this.checkMain = true;
    //         }
    //     }
    //     console.log("AiJPEG", "setImageContnet: 완성 size =" + this.pictureList.length);
    //     this.checkPictureList = true;
    //     return true;
    // }

        /**
     * Initialize ImageContent after reset - Creating ImageContent during file parsing
     */
    setContent(_pictureList) {
        this.init();
        // Frame-only pictureList
        this.pictureList = _pictureList;
        this.pictureCount = _pictureList.length;
        this.mainPicture = this.pictureList[0];
        this.checkPictureList = true;
        this.checkMain = true;
    }



    /**
     * Reset and initialize ImageContent - Creating a regular JPEG during file parsing
     */
    setBasicContent(sourceByteArray) {
        this.init();
        this.jpegMetaData = this.extractJpegMeta(sourceByteArray, ContentAttribute.basic);
        // No separation of APP1 data
        var app1Bytes = this.extractAPP1(sourceByteArray);
        var frameBytes = this.extractFrame(sourceByteArray, ContentAttribute.basic);
        // Create Picture object
       // var picture = new Picture(ContentAttribute.basic, app1Bytes, frameBytes);
        let picture = new Picture(null, app1Bytes, frameBytes, ContentAttribute.basic, 0, null)
        picture.waitForByteArrayInitialized();
        this.insertPicture(picture);
        this.mainPicture = pictureList[0];
        this.checkPictureList = true;
        this.checkMain = true;
    }

    getFrameStartPos(jpegBytes){
        var startIndex = 0;
        var SOFList = this.getSOFMarkerPosList(jpegBytes);
        if(SOFList.size > 0 ){
            startIndex = SOFList.pop()
        } else{
            startIndex = 0
        }
        return startIndex
    }

    insertPicture(picture){
        this.pictureList.push(picture);
        this.pictureCount++;
    }

        /**
     * metaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JPEG 파일의 Bytes를 리턴하는 함수
     */
    // // TODO: ("APP1 삭제 후 변경 필요")
    // getJpegBytes(picture) {
    //     console.log("getJpegBytes : 호출");
    //     //while (!checkPictureList) { }
    //     var buffer = new Uint8Array(0);
    //     var jpegMetaData = this.jpegMetaData;
    //     buffer = new Uint8Array(jpegMetaData.length + picture.imageSize + 2);
    //     buffer.set(jpegMetaData, 0);
    //     buffer.set(picture._pictureByteArray, jpegMetaData.length);
    //     buffer[jpegMetaData.length + picture.imageSize] = 0xFF;
    //     buffer[jpegMetaData.length + picture.imageSize + 1] = 0xD9;
    //     return buffer;
    // }

    // TODO: ("APP1 삭제 후 변경 필요")
    getBlobURL(picture) {
        console.log("getJpegBytes : 호출");
        
        var buffer = new Uint8Array(picture._metaData.length + picture.imageSize + 2);
        buffer.set(picture._metaData, 0);
        buffer.set(picture._pictureByteArray, picture._metaData.length);
        buffer[picture._metaData.length + picture.imageSize] = 0xFF;
        buffer[picture._metaData.length + picture.imageSize + 1] = 0xD9;
         
        // 버퍼 데이터를 Blob으로 변환
        var blob = new Blob([buffer], { type: "image/jpeg" });

        // Blob URL 생성
        var blobUrl = URL.createObjectURL(blob);
        return blobUrl
    }

   
    // getChagedJpegBytes(picture) {
    //     var newJpegMetaData = null;
    //     console.log("getChagedJpegBytes: 호출");
    //     while (!checkPictureList) { }
    //     // Change metadata
    //     if (!picture._app1Segment || picture._app1Segment.length <= 0) {
    //         newJpegMetaData = jpegMetaData;
    //     } else {
    //         newJpegMetaData = this.chageMetaData(picture._app1Segment);
    //     }
    //     // var buffer = new ArrayBuffer(newJpegMetaData.length + picture.imageSize + 2);
    //     var buffer = new Uint8Array(newJpegMetaData.length + picture.imageSize + 2);
    //     buffer.set(newJpegMetaData);
    //     buffer.set(picture._pictureByteArray, newJpegMetaData.length);
    //     buffer.set([0xff, 0xd9], newJpegMetaData.length + picture.imageSize);
        
    //     return buffer;
    // }

    getSOFMarkerPosList(jpegBytes) {
        var EOIPosList = this.getEOIMarekrPosList(jpegBytes);
        var SOFStartInex = 0;
        var SOFList = [];
        while (SOFStartInex < jpegBytes.length / 2 - 1) {
            var countFindingEOI = 0;
            if (jpegBytes[SOFStartInex] === 0xFF && jpegBytes[SOFStartInex + 1] === 0xC0) {
                SOFList.push(SOFStartInex);
                console.log("SOF LIST[" + SOFList.length + "] " + SOFStartInex);
            }
            SOFStartInex++;
            if (EOIPosList.length > 0) {
                if (SOFStartInex === EOIPosList[EOIPosList.length - 1]) {
                    break;
                }
            }
        }
        return SOFList;
    }

    findMCFormat(jpegBytes) {
        var app3StartIndex = 0;
        var app3DataLength = 0;
    
        while (app3StartIndex < jpegBytes.length / 2 - 1) {
            if (jpegBytes[app3StartIndex] === 0xFF && jpegBytes[app3StartIndex + 1] === 0xE3) {
                // MC Format 확인
                if (
                    (jpegBytes[app3StartIndex + 4] === 0x4D && jpegBytes[app3StartIndex + 5] === 0x43 && jpegBytes[app3StartIndex + 6] === 0x46) ||
                    (jpegBytes[app3StartIndex + 4] === 0x41 && jpegBytes[app3StartIndex + 5] === 0x69 && jpegBytes[app3StartIndex + 6] === 0x46)
                ) {
                    app3DataLength = (jpegBytes[app3StartIndex + 2] << 8) | jpegBytes[app3StartIndex + 3];
                    break;
                }
            }
            app3StartIndex++;
        }
    
        if (app3StartIndex === jpegBytes.length / 2 - 1) {
            app3StartIndex = 0;
        }
    
        return [app3StartIndex, app3DataLength];
    }

    /**
     * Return the metadata portion of a complete JPEG file consisting of metaData and Picture's byteArray (frame)
     */
    extractJpegMeta(bytes, attribute) {
        var metaDataEndPos = 0;
        var SOFList = [];
    
       // SOFList = this.getSOFMarkerPosList(bytes);
        metaDataEndPos = this.getFrameStartPos(bytes)

        
       // var byteBuffer = new Uint8Array();
        var resultByte = bytes.subarray(0, metaDataEndPos);
        //  // Ai JPEG Format 알 땨
        // if (APP3StartIndx > 0) {
        //      // APP3 (Ai jpeg) 영역을 제외하고 metadata write 
        //     var metaDataChunk = new Uint8Array(bytes.subarray(0, metaDataEndPos));
        //     //var afterApp3Chunk = new Uint8Array(bytes.subarray(APP3StartIndx + APP3DataLength, SOFList[SOFList.length - 1]));

        //     byteBuffer = new ArrayBuffer(metaDataChunk.length);
        //     var combinedBuffer = new Uint8Array(byteBuffer);
        //     combinedBuffer.set(metaDataChunk, 0);
        //     combinedBuffer.set(afterApp3Chunk, metaDataChunk.length);

        //     resultByte = combinedBuffer;
        // } else {
        //    // console.log("[meta]extract_metadata : 일반 JEPG처럼 저장 pos : " + metaDataEndPos);
        //     resultByte = bytes.subarray(0, metaDataEndPos);
        // }
        console.log("[meta] 추출한 메타데이터 사이즈 " + resultByte.length);
        return resultByte;
    }


    extractAPP1(allBytes) {
        var pos = 0;
        var app1StartPos = 0;
        var app1DataSize = 0;
        var findAPP1 = false;
        var byteBuffer = new Uint8Array();
    
        while (pos < allBytes.length - 1) {
            if (allBytes[pos] === 0xFF && allBytes[pos + 1] === 0xE1) {
                findAPP1 = true;
                app1StartPos = pos;
                app1DataSize = ((allBytes[pos + 2] & 0xFF) << 8) | (allBytes[pos + 3] & 0xFF);
                break;
            }
            pos++;
        }
    
        if (findAPP1) {
            console.log("app1StartPos : " + app1StartPos + ", app1DataSize : " + app1DataSize);
            byteBuffer = allBytes.subarray(app1StartPos, app1StartPos + app1DataSize + 2);
        } else {
            return new Uint8Array();
        }
    
        return byteBuffer;
    }
    
    extractSOI(jpegBytes) {
        return jpegBytes.subarray(2);
    }
    
    extractFrame(jpegBytes, attribute) {
        var pos = 0;
        var startIndex = 0;
        var endIndex = jpegBytes.length;
    
         // Frame Start pos 찾기
        var frameStartPos = this.getFrameStartPos(jpegBytes)
    
        pos = jpegBytes.length - 2;
        while (pos > 0) {
            if (jpegBytes[pos] === 0xFF && jpegBytes[pos + 1] === 0xD9) {
                endIndex = pos;
                break;
            }
            pos--;
        }
            //console.log("startIndex", "bytes[" + jpegBytes.length + "], start[" + startIndex + "], end[" + endIndex + "]");
        var resultByte = jpegBytes.subarray(frameStartPos, endIndex);
        return resultByte;    
        
    }

    
    getEOIMarekrPosList(jpegBytes) {
        var EOIStartInex = 0;
        var EOIList = [];
        while (EOIStartInex < jpegBytes.length - 1) {
            if (jpegBytes[EOIStartInex] === 0xFF && jpegBytes[EOIStartInex + 1] === 0xD9) {
                EOIList.push(EOIStartInex);
                console.log("EOIStartInex LIST[" + EOIList.length + "] " + EOIStartInex);
            }
            EOIStartInex++;
        }
        return EOIList;
    }
    
   
    
}
