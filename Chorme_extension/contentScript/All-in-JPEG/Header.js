import ImageContentInfo from "./content-info/ImageContentInfo.js";
import TextContentInfo from "./content-info/TextContentInfo.js";
import AudioContentInfo from "./content-info/AudioContentInfo.js";

export default class Header {
    constructor(_Ai_container) {
        this.headerDataLength = 0;
        this.AiContainer = _Ai_container;
        this.imageContentInfo = null;
        this.audioContentInfo = null;
        this.textContentInfo = null;
    }

    static get APP3_MARKER_SIZE() {
        return 2;
    }

    static get APP3_LENGTH_FIELD_SIZE() {
        return 2;
    }

    static get IDENTIFIER_FIELD_SIZE() {
        return 4;
    }

    settingHeaderInfo() {
        this.imageContentInfo = new ImageContentInfo(this.AiContainer.imageContent);
        this.textContentInfo = new TextContentInfo(this.AiContainer.textContent, this.imageContentInfo.getEndOffset());
        this.audioContentInfo = new AudioContentInfo(this.AiContainer.audioContent, this.textContentInfo.getEndOffset());
        this.headerDataLength = this.getAPP3FieldLength();
        this.applyAddedAPP3DataSize();
    }

    applyAddedAPP3DataSize() {
        const headerLength = this.getAPP3FieldLength() + 2;
        const jpegMetaLength = this.AiContainer.getJpegMetaBytes().length;
        
        for (let i = 0; i < this.imageContentInfo.imageCount; i++) {
            const pictureInfo = this.imageContentInfo.imageInfoList[i];
            if (i === 0) {
                pictureInfo.imageDataSize += headerLength + jpegMetaLength + 3;
            } else {
                pictureInfo.offset += headerLength + jpegMetaLength + 2;
            }
        }

        for (let i = 0; i < this.textContentInfo.textCount; i++) {
            const textInfo = this.textContentInfo.textInfoList[i];
            textInfo.offset += headerLength + jpegMetaLength + 2;
        }

        this.audioContentInfo.dataStartOffset += headerLength + jpegMetaLength + 2;
    }

    getAPP3FieldLength() {
        let size = this.getAPP3CommonDataLength();
        size += this.imageContentInfo.getLength();
        size += this.textContentInfo.getLength();
        size += this.audioContentInfo.getLength();
        return size;
    }

    getAPP3CommonDataLength() {
        return Header.APP3_MARKER_SIZE + Header.APP3_LENGTH_FIELD_SIZE + Header.IDENTIFIER_FIELD_SIZE;
    }

    convertBinaryData() {
        const buffer = new Uint8Array(this.getAPP3FieldLength() + 2);
        buffer[0] = 0xFF;
        buffer[1] = 0xE3;
        buffer.set(new Uint8Array(this.headerDataLength), 2);
        buffer.set([0x41, 0x69, 0x46, 0x00], 4);
        buffer.set(this.imageContentInfo.convertBinaryData(), 8);
        buffer.set(this.textContentInfo.convertBinaryData(), 8 + this.imageContentInfo.getLength());
        buffer.set(this.audioContentInfo.convertBinaryData(), 8 + this.imageContentInfo.getLength() + this.textContentInfo.getLength());
        return buffer;
    }
}
