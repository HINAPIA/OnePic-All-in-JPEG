import ContentAttribute from "../content/contentType.js"
export default class AudioContentInfo {
    constructor(audioContent, audioDataStartOffset) {
        this.contentInfoSize = 0;
        this.dataStartOffset = 0;
        this.datasize = 0;
        this.attribute = 0;
        this.FIELD_SIZE = 4;
        this.XOA_MARKER_SIZE = 2;

        this.init(audioContent, audioDataStartOffset);
    }

    init(audioContent, audioDataStartOffset) {
        this.contentInfoSize = this.FIELD_SIZE * 4;
        this.dataStartOffset = audioDataStartOffset;

        if (audioContent.aiAudio !== null) {
            this.attribute = audioContent.aiAudio.attribute;
            this.datasize = this.XOA_MARKER_SIZE + audioContent.aiAudio._audioByteArray.length;
        } else {
            this.attribute = ContentAttribute.basic.code;
            this.datasize = 0;
        }
    }

    convertBinaryData() {
        const buffer = new ArrayBuffer(this.contentInfoSize);
        const dataView = new DataView(buffer);

        dataView.setInt32(0, this.contentInfoSize);
        dataView.setInt32(4, this.dataStartOffset);
        dataView.setInt32(8, this.attribute);
        dataView.setInt32(12, this.datasize);

        return new Uint8Array(buffer);
    }

    getLength() {
        return this.contentInfoSize;
    }
}
