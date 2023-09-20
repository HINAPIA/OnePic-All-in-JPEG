import Header from "./Header.js";
import ImageContent from "./content/ImageContent.js";
import TextContent from "./content/TextContent.js";
import AudioContent from "./content/AudioContent.js";
import JpegConstant from "./JpegConstant.js";

export default class AiContainer {
    constructor() {
        this.header = new Header(this);

        this.imageContent = new ImageContent();
        this.audioContent = new AudioContent();
        this.textContent = new TextContent();

       // this.audioResolver = new AiaudioResolver();
        this.groupCount = 0;
        this.jpegConstant = new JpegConstant();

        this.isBurst = true;
        this.isAllinJPEG = true;
    }


    init() {
        this.imageContent.init();
        this.audioContent.init();
        this.textContent.init();
    }

    getPictureList() {
        while (!this.imageContent.checkPictureList) {}
        return this.imageContent.pictureList;
    }

    getPictureList(attribute) {
        const pictureList = this.imageContent.pictureList;
        const resultPictureList = [];
        for (let i = 0; i < pictureList.length; i++) {
            const picture = pictureList[i];
            if (picture.contentAttribute === attribute)
                resultPictureList.push(picture);
        }
        return resultPictureList;
    }

    getMainPicture() {
        return this.imageContent.mainPicture;
    }

    // overwiteSave(fileName) {
    //     return this.saveResolver.overwriteSave(fileName);
    // }

    // save() {
    //     return this.saveResolver.save();
    // }

    async setImageContent(byteArrayList, type, contentAttribute) {
        const jop = async () => {
            await this.imageContent.setContent(byteArrayList, contentAttribute);
        };
        await jop();
        return true;
    }

    setAudioContent(audioBytes, contentAttribute) {
        this.audioContent.setContent(audioBytes, contentAttribute);
    }

    setTextConent(contentAttribute, textList) {
        this.textContent.setContent(contentAttribute, textList);
    }

    setBasicJepg(sourceByteArray) {
        this.init();
        this.imageContent.setBasicContent(sourceByteArray);
    }

    settingHeaderInfo() {
        this.header.settingHeaderInfo();
    }

    convertHeaderToBinaryData() {
        return this.header.convertBinaryData();
    }

    // 음악 재생 시작
    playAudio(){
        this.audioContent.playAudio();
    }

    createAudio(){
        this.audioContent.createNewAudioPlayer();
    }

    // audioPlay() {
    //     const audio = this.audioContent.audio;
    //     if (audio !== null) {
    //         this.audioResolver.audioPlay(audio);
    //     }
    // }

    // audioStop() {
    //     const audio = this.audioContent.audio;
    //     if (audio !== null) {
    //         this.audioResolver.audioStop();
    //     }
    // }

    getJpegMetaBytes() {
        if (this.imageContent.jpegMetaData.length === 0) {
            //console.error("JpegMetaData size가 0입니다.");
        }
        return this.imageContent.jpegMetaData;
    }

    setJpegMetaBytes(_jpegMetaData) {
        this.imageContent.jpegMetaData = _jpegMetaData;
    }
}

