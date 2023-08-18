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

    async setContent(byteArrayList, contentAttribute) {
        this.init();
        this.jpegMetaData = this.extractJpegMeta(byteArrayList[0], contentAttribute);
        for (let i = 0; i < byteArrayList.length; i++) {
            let app1Bytes = this.extractAPP1(byteArrayList[i]);
            let frameBytes = await this.extractFrame(byteArrayList[i], contentAttribute);
            //let picture = new Picture(contentAttribute, app1Bytes, frameBytes);
            let picture = new Picture(null, app1Bytes, frameBytes, contentAttribute, 0, null)
            picture.waitForByteArrayInitialized();
            this.insertPicture(picture);
            console.log("AiJPEG", "setImageContnet: picture[" + i + "] 완성");
            if (i === 0) {
                this.mainPicture = picture;
                this.checkMain = true;
            }
        }
        console.log("AiJPEG", "setImageContnet: 완성 size =" + this.pictureList.length);
        this.checkPictureList = true;
        return true;
    }

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
        jpegMetaData = this.extractJpegMeta(sourceByteArray, ContentAttribute.basic);
        // No separation of APP1 data
        var app1Bytes = this.extractAPP1(sourceByteArray);
        var frameBytes = this.extractFrame(sourceByteArray, ContentAttribute.basic);
        // Create Picture object
       // var picture = new Picture(ContentAttribute.basic, app1Bytes, frameBytes);
       let picture = new Picture(null, app1Bytes, frameBytes, ContentAttribute.basic, 0, null)
        picture.waitForByteArrayInitialized();
        this.insertPicture(picture);
        mainPicture = pictureList[0];
        checkPictureList = true;
        checkMain = true;
    }

        /**
     * metaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JPEG 파일의 Bytes를 리턴하는 함수
     */
    // // TODO: ("APP1 삭제 후 변경 필요")
    getJpegBytes(picture) {
        console.log("getJpegBytes : 호출");
        //while (!checkPictureList) { }
        var buffer = new Uint8Array(0);
        var jpegMetaData = this.jpegMetaData;
        buffer = new Uint8Array(jpegMetaData.length + picture.imageSize + 2);
        buffer.set(jpegMetaData, 0);
        buffer.set(picture._pictureByteArray, jpegMetaData.length);
        buffer[jpegMetaData.length + picture.imageSize] = 0xFF;
        buffer[jpegMetaData.length + picture.imageSize + 1] = 0xD9;
        return buffer;
    }

    // TODO: ("APP1 삭제 후 변경 필요")
    getBlobURL(picture) {
        console.log("getJpegBytes : 호출");
        
        // while (!this.checkPictureList) { 
        //     await new Promise(resolve => setTimeout(resolve, 100));
        // }ƒ
        var buffer = new Uint8Array(0);
        var jpegMetaData = this.jpegMetaData;
        buffer = new Uint8Array(jpegMetaData.length + picture.imageSize + 2);
        buffer.set(jpegMetaData, 0);
        buffer.set(picture._pictureByteArray, jpegMetaData.length);
        buffer[jpegMetaData.length + picture.imageSize] = 0xFF;
        buffer[jpegMetaData.length + picture.imageSize + 1] = 0xD9;
         
        // 버퍼 데이터를 Blob으로 변환
        var blob = new Blob([buffer], { type: "image/jpeg" });

        // Blob URL 생성
        var blobUrl = URL.createObjectURL(blob);
        return blobUrl
    }

    /**
     * 기존 metadata의 APP1 segment를 newApp1Data로 교체 후 변경된 metadata 리턴
     */
    chageMetaData(newApp1Data) {
        var pos = 0;
        var app1DataSize = 0;
        var app1StartPos = 0;
        var findAPP1 = false;
        var jpegMetaData = new Uint8Array(/* Initialize this with your metaData array content */);
        var byteBuffer = new Uint8Array(0);

        while (pos < jpegMetaData.length) {
            // APP1 마커 위치 찾기
            if (jpegMetaData[pos] === 0xFF && jpegMetaData[pos + 1] === 0xE1) {
                app1DataSize = (jpegMetaData[pos + 2] << 8) | jpegMetaData[pos + 3];
                app1StartPos = pos;
                findAPP1 = true;
                break;
            }
            pos++;
        }

        if (findAPP1) {
            // 기존 APP1 segment를 newApp1Data로 교체 후 리턴
            byteBuffer = new Uint8Array(jpegMetaData.length - app1DataSize + newApp1Data.length);
            byteBuffer.set(jpegMetaData.subarray(0, app1StartPos), 0);
            byteBuffer.set(newApp1Data, app1StartPos);
            byteBuffer.set(
                jpegMetaData.subarray(app1StartPos + app1DataSize + 2),
                app1StartPos + newApp1Data.length
            );
            return byteBuffer;
        } else {
            // 교체 안함
            return jpegMetaData;
        }
    }

    getChagedJpegBytes(picture) {
        var newJpegMetaData = null;
        console.log("getChagedJpegBytes: 호출");
        while (!checkPictureList) { }
        // Change metadata
        if (!picture._app1Segment || picture._app1Segment.length <= 0) {
            newJpegMetaData = jpegMetaData;
        } else {
            newJpegMetaData = this.chageMetaData(picture._app1Segment);
        }
        // var buffer = new ArrayBuffer(newJpegMetaData.length + picture.imageSize + 2);
        var buffer = new Uint8Array(newJpegMetaData.length + picture.imageSize + 2);
        buffer.set(newJpegMetaData);
        buffer.set(picture._pictureByteArray, newJpegMetaData.length);
        buffer.set([0xff, 0xd9], newJpegMetaData.length + picture.imageSize);
        
        return buffer;
    }

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
        console.log("extractJpegMeta =============================");
        var isFindStartMarker = false;
        var metaDataEndPos = 0;
        var SOFList = [];
        var APP0MarkerList = [];
        SOFList = this.getSOFMarkerPosList(bytes);

        if (attribute === ContentAttribute.edited || attribute === ContentAttribute.magic) {
            APP0MarkerList = this.findAPP0Makers(bytes);
            if (APP0MarkerList.length > 0) {
                isFindStartMarker = true;
                metaDataEndPos = this.APP0MarkerList[APP0MarkerList.length - 1];
            }
        }

        if (!isFindStartMarker) {
            if (SOFList.length > 0) {
                metaDataEndPos = SOFList[SOFList.length - 1];
            } else {
                console.log("[meta]extract metadata : SOF가 존재하지 않음");
                return new Uint8Array(0);
            }
        }

        var resultArray = this.findMCFormat(bytes);
        var APP3StartIndx = resultArray[0];
        var APP3DataLength = resultArray[1];
        console.log("[meta]APP3StartIndx : " + APP3StartIndx + ", APP3DataLength : " + APP3DataLength);
        var resultByte;
        var byteBuffer = new Uint8Array();
        if (APP3StartIndx > 0) {
             // APP3 (Ai jpeg) 영역을 제외하고 metadata write
            console.log("[meta]extract_metadata : MC 포맷이여서 APP3 메타 데이터 뺴고 저장");
            
            var metaDataChunk = new Uint8Array(bytes.subarray(0, APP3StartIndx));
            var afterApp3Chunk = new Uint8Array(bytes.subarray(APP3StartIndx + APP3DataLength, SOFList[SOFList.length - 1]));

            byteBuffer = new ArrayBuffer(metaDataChunk.length + afterApp3Chunk.length);
            var combinedBuffer = new Uint8Array(byteBuffer);
            combinedBuffer.set(metaDataChunk, 0);
            combinedBuffer.set(afterApp3Chunk, metaDataChunk.length);

            resultByte = combinedBuffer;
        } else {
            console.log("[meta]extract_metadata : 일반 JEPG처럼 저장 pos : " + metaDataEndPos);
            resultByte = bytes.subarray(0, metaDataEndPos);
        }
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
    
     isBasicPicture(bytes) {
        var findApp0 = false;
        var findApp1 = false;
        var pos = 0;
        while (pos < bytes.length - 1) {
            if (bytes[pos] === 0xFF && bytes[pos + 1] === 0xE1) {
                findApp1 = true;
            }
            if (bytes[pos] === 0xFF && bytes[pos + 1] === 0xE0) {
                findApp0 = true;
            }
            pos++;
        }
    
        return !findApp0 && findApp1;
    }
    
    extractSOI(jpegBytes) {
        return jpegBytes.subarray(2);
    }
    
    extractFrame(jpegBytes, attribute) {
        var pos = 0;
        var startIndex = 0;
        var endIndex = jpegBytes.length;
    
        var isFindStartMarker = false;
        var isFindEndMarker = false;
    
        var SOFList = [];
        var APP0MarkerList = [];
    
        if (attribute === ContentAttribute.edited || attribute === ContentAttribute.magic) {
            APP0MarkerList = this.findAPP0Makers(jpegBytes);
            if (APP0MarkerList.length > 0) {
                isFindStartMarker = true;
                startIndex = APP0MarkerList[APP0MarkerList.length - 1];
                console.log("extract frame : JFIF 찾음 " + startIndex);
            }
        }
    
        if (!isFindStartMarker) {
            SOFList = this.getSOFMarkerPosList(jpegBytes);
            if (SOFList.length > 0) {
                isFindStartMarker = true;
                startIndex = SOFList[SOFList.length - 1];
                console.log("extract frame : SOF 찾음 " + startIndex);
            } else {
                console.log("extract frame : SOF가 존재하지 않음");
                return new Uint8Array();
            }
        }
    
        pos = jpegBytes.length - 2;
        while (pos > 0) {
            if (jpegBytes[pos] === 0xFF && jpegBytes[pos + 1] === 0xD9) {
                endIndex = pos;
                isFindEndMarker = true;
                break;
            }
            pos--;
        }
    
        if (!isFindStartMarker || !isFindEndMarker) {
            console.log("Error: 찾는 마커가 존재하지 않음");
            return new Uint8Array();
        } else {
            var resultByte;
            console.log("startIndex", "bytes[" + jpegBytes.length + "], start[" + startIndex + "], end[" + endIndex + "]");
            resultByte = jpegBytes.subarray(startIndex, endIndex);
            return resultByte;
        }
    }

    findAPP0Makers(jpegBytes) {
        var JFIF_startOffset = 0;
        var JFIFList = [];
        while (JFIF_startOffset < jpegBytes.length - 1) {
            if (jpegBytes[JFIF_startOffset] === 0xFF && jpegBytes[JFIF_startOffset + 1] === 0xE0) {
                JFIFList.push(JFIF_startOffset);
                console.log("extract metadata :  JIFI찾음 - " + JFIF_startOffset);
            }
            JFIF_startOffset++;
        }
        return JFIFList;
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
