export default class Picture {
 

    // 새로운 생성자 추가
    constructor(offset = null, metaData, pictureByteArray, contentAttribute = null, embeddedSize = 0, embeddedData = null) {
        this.contentAttribute = contentAttribute;
        this._metaData = metaData ? new Uint8Array(metaData) : null;
        this._pictureByteArray = new Uint8Array(pictureByteArray);
        this.imageSize = this._pictureByteArray.length;
        this.embeddedSize = embeddedSize;
        this.embeddedData = embeddedData;
        this.offset = offset;

        if (pictureByteArray) {
            this._pictureByteArray = new Uint8Array(pictureByteArray);
            this.imageSize = pictureByteArray.length;
            pictureByteArray = null;
        }
        // if (metaData) {
        //     this._metaData = new Uint8Array(metaData);
        // }
    }

    insertEmbeddedData(data) {
        this.embeddedData = data;
        this.embeddedSize = data.length * 4;
    }


    async waitForByteArrayInitialized() {
        while (!this.isByteArrayInitialized()) {
            await new Promise(resolve => setTimeout(resolve, 100));
        }
    }

    isByteArrayInitialized() {
        return this._pictureByteArray !== null;
    }
}
