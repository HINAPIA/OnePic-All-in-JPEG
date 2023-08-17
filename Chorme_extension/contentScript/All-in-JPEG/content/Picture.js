export default class Picture {
    // constructor(contentAttribute, app1Segment = null, pictureByteArray = null) {
    //     this.contentAttribute = contentAttribute;
    //     this._app1Segment = null;
    //     this._pictureByteArray = null;
    //     this.imageSize = pictureByteArray ? pictureByteArray.length : 0;
    //     this.embeddedSize = 0;
    //     this.embeddedData = null;
    //     this.offset = 0;

    //     if (pictureByteArray) {
    //         this._pictureByteArray = new Uint8Array(pictureByteArray);
    //         this.imageSize = pictureByteArray.length;
    //         pictureByteArray = null;
    //     }
    //     if (app1Segment) {
    //         this._app1Segment = new Uint8Array(app1Segment);
    //     }
    // }

    // 새로운 생성자 추가
    constructor(offset = null, app1Segment, pictureByteArray, contentAttribute = null, embeddedSize = 0, embeddedData = null) {
        this.contentAttribute = contentAttribute;
        this._app1Segment = app1Segment ? new Uint8Array(app1Segment) : null;
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
        if (app1Segment) {
            this._app1Segment = new Uint8Array(app1Segment);
        }
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
