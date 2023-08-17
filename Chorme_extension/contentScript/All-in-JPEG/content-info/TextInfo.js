export default class TextInfo {
    constructor(text) {
        this.offset = 0;
        this.dataSize = 0;
        this.data = '';
        this.attribute = 0;

        this.init(text);
    }

    init(text) {
        this.dataSize = text.data.length * 2;
        this.data = text.data;
        this.attribute = text.contentAttribute.code;
    }

    getTextInfoSize() {
        // Int(4) * 3
        return 4 * 3;
    }
}
