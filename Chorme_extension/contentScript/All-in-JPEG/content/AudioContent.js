import Audio from "./Audio.js";
export default class AudioContent {
    constructor() {
        this.audio = null;
    }

    init() {
        this.audio = null;
    }

    setContent(byteArray, contentAttribute) {
        this.init();
        console.log("audio_test", "audio SetContent, audio 객체 갱신");

        // audio 객체 생성
        this.audio = new Audio(byteArray, contentAttribute);
        this.audio.waitForByteArrayInitialized();

        console.log("audio_test", `setContent: 오디오 크기 ${this.audio.size}`);
    }

    setContentFromAudio(_audio) {
        this.init();
        this.audio = _audio;
        this.audio.waitForByteArrayInitialized();
    }
}
