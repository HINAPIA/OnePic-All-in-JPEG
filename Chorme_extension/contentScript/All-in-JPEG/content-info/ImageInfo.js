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

    init(picture) {
        
        this.app1DataSize = picture._app1Segment ? picture._app1Segment.length : 0;
        this.imageDataSize = picture.imageSize;
        this.attribute = picture.contentAttribute.code;
        this.embeddedDataSize = picture.embeddedSize;

        if (this.embeddedDataSize > 0) {
            this.embeddedData = picture.embeddedData;
        }
    }

    getImageInfoSize() {
        return 20 + this.embeddedDataSize;
    }
}
