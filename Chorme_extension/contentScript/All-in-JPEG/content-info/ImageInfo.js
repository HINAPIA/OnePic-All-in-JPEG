export default class ImageInfo {
    constructor(picture) {
        this.metaDataSize = 0;
        this.offset = 0;
        this.imageDataSize = 0;
        this.attribute = 0;
        this.embeddedDataSize = 0;
        this.embeddedData = [];

        this.init(picture);
    }

    init(picture) {
        
        this.metaDataSize = picture._mataData ? picture._mataData.length : 0;
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
