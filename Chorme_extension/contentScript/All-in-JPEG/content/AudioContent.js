import Aiaudio from "./Aiaudio.js";
export default class AudioContent {
    constructor() {
        this.aiAudio = null;
        this.audioPlayer = null;
        this.blobUrl = null;
    }

    init() {
        this.aiAudio = null;
    }

    setContent(byteArray, contentAttribute) {
        this.init();
        console.log("audio_test", "audio SetContent, audio 객체 갱신");

        // audio 객체 생성
        this.aiAudio = new Aiaudio(byteArray, contentAttribute);
        this.aiAudio.waitForByteArrayInitialized();

        console.log("audio_test", `setContent: 오디오 크기 ${this.aiAudio.size}`);
    }

    setContentFromAudio(_audio) {
        this.init();
        this.aiAudio = _audio;
        this.aiAudio.waitForByteArrayInitialized();
    }

    // 음억 재생
    // 음악 파일 재생 함수
    playAudio() {
        if(this.aiAudio){
            if(!this.audioPlayer){
                this.audioPlayer = this.createNewAudioPlayer()
            }
            console.log("오디오 재생 시작.")
            this.audioPlayer.play();
        }
    }

    createNewAudioPlayer(){
        if(this.aiAudio != null){
            const blob = new Blob([this.aiAudio._audioByteArray], { type: 'audio/mp3' });
            this.blobUrl = URL.createObjectURL(blob);
            console.log("새로운 오디오 파일 갱신.")
            return new Audio(this.blobUrl);
        }
        else{
            console.log("오디오 파일 갱신 실패.")
       }
    }

    // 음악 중단 함수
    pauseAudio() {
        if (this.audioPlayer) {
            this.audioPlayer.pause();
        }
    }

    // 음악 중단 및 재생 중인 위치 초기화 함수
    stopAudio() {
        if (this.audioPlayer) {
            this.audioPlayer.pause();
            this.audioPlayer.currentTime = 0;
        }
    }

}
