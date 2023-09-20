import ContentAttribute from "./content/contentType.js";
import Picture from "./content/Picture.js";
import Aiaudio from "./content/Aiaudio.js";
import Text from "./content/Text.js";

export default class LoadResolver {
    static MARKER_SIZE = 2;
    static APP3_FIELD_LENGTH_SIZE = 2;
    static FIELD_SIZE = 4;
    static BURST_MODE_SIZE = 1;
    
    async isAllinJPEG(sourceByreArray){
        var APP3_startOffset = 2
        APP3_startOffset =  await this.findAPP3StartPos(sourceByreArray)
        if(APP3_startOffset == -1)

            return false;
        else 
            return true;
    }


    async findAPP3StartPos(sourceByteArray) {
        let APP3_startOffset = 2;
        while (APP3_startOffset < sourceByteArray.length - 1) {
            if (sourceByteArray[APP3_startOffset] === 0xFF && sourceByteArray[APP3_startOffset + 1] === 0xE3) {
                if (
                    (sourceByteArray[APP3_startOffset + 4] === 0x4D && sourceByteArray[APP3_startOffset + 5] === 0x43 &&
                        sourceByteArray[APP3_startOffset + 6] === 0x46) ||
                    (sourceByteArray[APP3_startOffset + 4] === 0x41 && sourceByteArray[APP3_startOffset + 5] === 0x69 &&
                        sourceByteArray[APP3_startOffset + 6] === 0x46)
                ) {
                    return APP3_startOffset;
                } else {
                    return -1;
                }
            }
            APP3_startOffset++;
        }
        return -1;
    }

    async createAiContainer(AiContainer, sourceByteArray) {

        console.log(`createAiContainer() sourceByreArray.Size : ${sourceByteArray.length}`);
        
        const APP3_startOffset = await this.findAPP3StartPos(sourceByteArray);
        
        if (APP3_startOffset === -1) {
            try {
                console.log("createAiContainer() 일반 JPEG 생성");
                AiContainer.setBasicJepg(sourceByteArray);
            } catch (e) {
                console.error("createAiContainer() Basic JPEG Parsing 불가", e);
            }
        } else {
            try {
                console.log("createAiContainer() MC JPEG 생성");
                console.log(`createAiContainer() App3 Start Offset : ${APP3_startOffset}`);
                //JpegViewModel.AllInJPEG = true;
                
                const isBurstMode = sourceByteArray[LoadResolver.APP3_startOffset + LoadResolver.MARKER_SIZE + LoadResolver.APP3_FIELD_LENGTH_SIZE + LoadResolver.FIELD_SIZE];
            

                // 1. Image Content Parsing
                const imageContentStartOffset = APP3_startOffset + LoadResolver.MARKER_SIZE + LoadResolver.APP3_FIELD_LENGTH_SIZE + LoadResolver.FIELD_SIZE + LoadResolver.BURST_MODE_SIZE;
                const imageContentInfoSize = this.ByteArraytoInt(sourceByteArray, imageContentStartOffset);
                const pictureList = await this.imageContentParsing(
                    AiContainer,
                    sourceByteArray,
                    sourceByteArray.slice(
                        imageContentStartOffset,
                        imageContentStartOffset + imageContentInfoSize
                    )
                );
                AiContainer.imageContent.setContent(pictureList);


                // 2. Text Conent Parsing
                const textContentStartOffset = APP3_startOffset + LoadResolver.MARKER_SIZE + LoadResolver.APP3_FIELD_LENGTH_SIZE + LoadResolver.FIELD_SIZE + LoadResolver.BURST_MODE_SIZE + imageContentInfoSize;
                const textContentInfoSize = this.ByteArraytoInt(sourceByteArray, textContentStartOffset);
                if (textContentInfoSize > 0) {
                    const textList = await this.textContentParsing(
                        AiContainer,
                        sourceByteArray,
                        sourceByteArray.slice(
                            textContentStartOffset,
                            textContentStartOffset + textContentInfoSize
                        )
                    );
                    AiContainer.textContent.setContentFromTextList(textList);
                }
                
                // 3. Audio Content Parsing
                const audioContentStartOffset = textContentStartOffset + textContentInfoSize;
                const audioContentInfoSize = this.ByteArraytoInt(sourceByteArray, audioContentStartOffset);
                console.log(`audioContentInfoSize : ${audioContentInfoSize}`);
                if (audioContentInfoSize > 0) {
                    const audioDataStartOffset = this.ByteArraytoInt(sourceByteArray, audioContentStartOffset + 4);
                    console.log(`audioDataStartOffset : ${audioDataStartOffset}`);

                    const audioAttribute = this.ByteArraytoInt(sourceByteArray, audioContentStartOffset + 8);
                    console.log(`audioAttribute : ${audioAttribute}`);

                    const audioDataLength = this.ByteArraytoInt(sourceByteArray, audioContentStartOffset + 12);
                    console.log(`audioDataLength : ${audioDataLength}`);
                    
                    if (audioDataLength > 0) {
                        const audioBytes = sourceByteArray.slice(
                            audioDataStartOffset + LoadResolver.MARKER_SIZE,
                            audioDataStartOffset + audioDataLength
                        );
                        console.log(`audioBytes : ${audioBytes.length}`);
                        
                        //let audio = new Aiaudio(audioBytes, ContentAttribute.fromCode(audioAttribute))
                        
                        AiContainer.audioContent.setContent(audioBytes, ContentAttribute.fromCode(audioAttribute));
                        // AiContainer.audioResolver.saveByteArrToAacFile(audioBytes, "viewer_record");
                    }
                }

            } catch (e) {
                console.error("MC JPEG Parsing 불가", e);
            }
        }
    }

    ByteArraytoInt(byteArray, stratOffset) {
        return (
            (byteArray[stratOffset] & 0xff) * 0x1000000 +
            (byteArray[stratOffset + 1] & 0xff) * 0x10000 +
            (byteArray[stratOffset + 2] & 0xff) * 0x100 +
            (byteArray[stratOffset + 3] & 0xff)
        );
    }

    async imageContentParsing(AiContainer, sourceByteArray, imageInfoByteArray) {
        const pictureList = [];
        let startIndex = 0;

        startIndex++;
        const imageCount = this.ByteArraytoInt(imageInfoByteArray, startIndex * 4);
        startIndex++;
    
        for (let i = 0; i < imageCount; i++) {
            const offset = this.ByteArraytoInt(imageInfoByteArray, startIndex * 4); startIndex++;
            const metaDataSize = this.ByteArraytoInt(imageInfoByteArray, startIndex * 4); startIndex++;
            const size = this.ByteArraytoInt(imageInfoByteArray, startIndex * 4); startIndex++;
            const attribute = this.ByteArraytoInt(imageInfoByteArray, startIndex * 4); startIndex++;
            const embeddedDataSize = this.ByteArraytoInt(imageInfoByteArray, startIndex * 4); startIndex++;
            
            const embeddedData = [];
            if (embeddedDataSize > 0) {
                for (let j = 0; j < embeddedDataSize / 4; j++) {
                    const curInt = this.ByteArraytoInt(imageInfoByteArray, startIndex * 4);
                    embeddedData.push(curInt);
                    startIndex++;
                }
            }
    
            let picture;
            if (i === 0) {
                const jpegBytes = sourceByteArray.slice(offset, offset + size - 1);
                // Jpeg Meta 데이터 떼기
                const jpegMetaData = AiContainer.imageContent.extractJpegMeta(
                    sourceByteArray.slice(offset, offset + size - 1),
                    ContentAttribute.fromCode(attribute)
                );
                AiContainer.setJpegMetaBytes(jpegMetaData);
            
                const frame = await AiContainer.imageContent.extractFrame(jpegBytes, ContentAttribute.fromCode(attribute));
                picture = new Picture(offset, jpegMetaData, frame, ContentAttribute.fromCode(attribute), embeddedDataSize, embeddedData);
                await picture.waitForByteArrayInitialized();
            } else {
                const metaData = sourceByteArray.slice(offset + 2, offset + 2 + metaDataSize);
                const imageData = sourceByteArray.slice(offset + 2 + metaDataSize, offset + 2 + metaDataSize + size);
                picture = new Picture(offset, metaData, imageData, ContentAttribute.fromCode(attribute), embeddedDataSize, embeddedData);
                await picture.waitForByteArrayInitialized();
            }
    
            pictureList.push(picture);
            console.log("Image_Parsing:", picture);
            console.log(`Load_Module: picutureList size: ${pictureList.length}`);
        }
    
        return pictureList;
    }
    
    async textContentParsing(AiContainer, sourceByteArray, textInfoByteArray) {
        const textList = [];
        let startIndex = 0;
        const textContentInfoSize = this.ByteArraytoInt(textInfoByteArray, startIndex);
        startIndex++;
        const textCount = this.ByteArraytoInt(textInfoByteArray, startIndex * LoadResolver.FIELD_SIZE);
        startIndex++;
    
        for (let i = 0; i < textCount; i++) {
            const offset = this.ByteArraytoInt(textInfoByteArray, startIndex * LoadResolver.FIELD_SIZE); startIndex++;
            const attribute = this.ByteArraytoInt(textInfoByteArray, startIndex * LoadResolver.FIELD_SIZE); startIndex++;
            const size = this.ByteArraytoInt(textInfoByteArray, startIndex * LoadResolver.FIELD_SIZE); startIndex++;
            
            const charArray = new Array(size / 2); // 변환된 char 값들을 담을 배열
            if (size > 0 && sourceByteArray[offset] === 0xFF && sourceByteArray[offset + 1] === 0x20) {
                for (let j = 0; j < size; j += 2) {
                    const charValue = ((sourceByteArray[offset + LoadResolver.MARKER_SIZE + j] & 0xFF) << 8) |
                        (sourceByteArray[offset + LoadResolver.MARKER_SIZE + j + 1] & 0xFF);
                    charArray[j / 2] = String.fromCharCode(charValue); // char로 변환 후 배열에 저장
                }
            }
            const string = charArray.join('');
            const text = new Text(string, ContentAttribute.fromCode(attribute));
            console.log(`text_parsing: ${text.toString()}`);
            textList.push(text);
        }
    
        return textList;
    }
    
}
