import Text from "./Text.js";

export default class TextContent {
    constructor() {
        this.textList = [];
        this.textCount = 0;
    }

    init() {
        this.textList = [];
        this.textCount = 0;
    }

    setContent(contentAttribute, textList) {
        this.init();
        if(textList){
            for (let i = 0; i < textList.length; i++) {
                let text = new Text(textList[i], contentAttribute);
                this.insertText(text);
            }
        }
    }

    setContentFromTextList(_textList) {
        this.init();
        this.textList = _textList;
        this.textCount = _textList.length;
    }

    insertText(text) {
        this.textList.push(text);
        this.textCount++;
    }

    getTextAtIndex(index) {
        if (index >= 0 && index < this.textList.length) {
            return this.textList[index];
        } else {
            return null;
        }
    }

    getAllText() {
        return this.textList;
    }
}
