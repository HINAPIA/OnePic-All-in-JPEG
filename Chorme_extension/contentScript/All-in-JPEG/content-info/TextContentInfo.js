
export default class TextContentInfo {
    constructor(textContent, startOffset) {
        this.contentInfoSize = 0;
        this.textCount = 0;
        this.textInfoList = [];
        this.dataStartOffset = 0;

        this.XOT_MARKER_SIZE = 2;
        this.TEXT_CONTENT_SIZE_FIELD_SIZE = 4;
        this.TEXT_COUNT_FIELD_SIZE = 4;
        this.OFFSET_FIELD_SIZE = 4;
        this.ATTRIBUTE_FIELD_SIZE = 4;
        this.TEXT_DATA_SIZE = 4;

        this.init(textContent, startOffset);
    }

    init(textContent, startOffset) {
        this.dataStartOffset = startOffset;
        this.textCount = textContent.textCount;
        this.textInfoList = this.fillTextInfoList(textContent.textList);
        this.contentInfoSize = this.getLength();
    }

    fillTextInfoList(textList) {
        let preSize = 0;
        let textInfoList = [];
        
        for (let i = 0; i < textList.length; i++) {
            let textInfo = new TextInfo(textList[i]);
            let textDataStartOffset = this.dataStartOffset;
            
            if (i > 0) {
                textDataStartOffset += preSize;
            }
            
            textInfo.offset = textDataStartOffset;
            preSize = this.XOT_MARKER_SIZE + textInfo.dataSize;
            
            textInfoList.push(textInfo);
        }
        
        return textInfoList;
    }

    convertBinaryData() {
        const buffer = new ArrayBuffer(this.getLength());
        const dataView = new DataView(buffer);

        dataView.setInt32(0, this.contentInfoSize);
        dataView.setInt32(4, this.textCount);

        let offset = 8;
        for (let j = 0; j < this.textCount; j++) {
            const textInfo = this.textInfoList[j];
            dataView.setInt32(offset, textInfo.offset);
            dataView.setInt32(offset + 4, textInfo.attribute);
            dataView.setInt32(offset + 8, textInfo.dataSize);
            offset += 12;
        }

        return new Uint8Array(buffer);
    }

    getLength() {
        let size = this.TEXT_CONTENT_SIZE_FIELD_SIZE + this.TEXT_COUNT_FIELD_SIZE;
        size += this.textCount * (this.OFFSET_FIELD_SIZE + this.ATTRIBUTE_FIELD_SIZE + this.TEXT_DATA_SIZE);
        return size;
    }

    getEndOffset() {
        if (this.textInfoList.length === 0) {
            return this.dataStartOffset;
        } else {
            const lastTextInfo = this.textInfoList[this.textInfoList.length - 1];
            return lastTextInfo.offset + this.XOT_MARKER_SIZE + lastTextInfo.dataSize;
        }
    }
}
