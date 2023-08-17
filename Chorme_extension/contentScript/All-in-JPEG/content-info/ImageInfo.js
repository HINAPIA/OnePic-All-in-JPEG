export default class ImageInfo {
    constructor(picture) {
        this.app1DataSize = 0;
        this.offset = 0;
        this.imageDataSize = 0;
        this.attribute = 0;
        this.embeddedDataSize = 0;
        this.embeddedData = [];

        this.init(picture);
    }

    init(Image) {
        this.app1DataSize = Image._app1Segment ? Image._app1Segment.length : 0;
        this.imageDataSize = Image.imageSize;
        this.attribute = Image.contentAttribute.code;
        this.embeddedDataSize = Image.embeddedSize;

        if (this.embeddedDataSize > 0) {
            this.embeddedData = Image.embeddedData;
        }
    }

    getImageInfoSize() {
        return 20 + this.embeddedDataSize;
    }
}
