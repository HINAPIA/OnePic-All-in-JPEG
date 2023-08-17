export default class Audio{
    constructor(audioByteArray, _Content_attribute) {
        this._audioByteArray = null;
        this.attribute = _Content_attribute;
        this.size = audioByteArray ? audioByteArray.length : 0;

        if (audioByteArray) {
            this._audioByteArray = new Uint8Array(audioByteArray);
            this.size = audioByteArray.length;
        }
    }

    async waitForByteArrayInitialized() {
        while (!this.isByteArrayInitialized()) {
            await new Promise(resolve => setTimeout(resolve, 100));
        }
    }

    isByteArrayInitialized() {
        return this._audioByteArray !== null;
    }
}
