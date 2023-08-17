import AiContainer from './All-in-JPEG/AiContainer.js';
import LoadResolver from './All-in-JPEG/LoadResolver.js';


document.addEventListener("DOMContentLoaded", function(event) { // 웹 페이지의 DOM이 로드되면 실행되는 코드
  console.log("DOM is loaded");
  // document가 로드되었을 때 ready 상태를 background.js에게 알립니다.
  chrome.runtime.sendMessage({ action: "ready" });
});


chrome.runtime.onMessage.addListener(function(message, sender, sendResponse) { // background.js로부터 메시지를 수신합니다.
  if (message.action === "displayImage") {
    console.log("Message received : displayImage")
    displayImage(message.url);
  }
});


async function displayImage(imageUrl) { // 이미지를 보여주는 함수를 정의합니다.
    const imageElement = document.getElementById("main_image")
    console.log(imageUrl+"이다!!!!!")
    imageElement.src = imageUrl;
    document.getElementById("file-name").innerText = getFileNameFromUrl(imageUrl)
    getImageByteArrayFromURL(imageUrl)
    .then(async byteArray => {
      if (byteArray) {
        console.log('Image Byte Array:', byteArray);
        let aiContainer = new AiContainer();
        let loadResolver = new LoadResolver();
        await loadResolver.createAiContainer(aiContainer, byteArray);
        

        // while(true){
        //   await new Promise(resolve => setTimeout(resolve, 1000));
        //   break;
        // }
        let testPicture = aiContainer.imageContent.pictureList[1]
        const testImageElement = document.getElementById("sub_image1");
        testImageElement.src = aiContainer.imageContent.getBlobURL(testPicture);

        testPicture = aiContainer.imageContent.pictureList[2]
        const testImageElement2 = document.getElementById("sub_image2");
        testImageElement2.src = aiContainer.imageContent.getBlobURL(testPicture);
      }
    });
}

function getFileNameFromUrl(imageUrl) {
  // 파일 경로를 "/" 또는 "\" 기준으로 분리하여 배열로 만듭니다.
  const pathParts = imageUrl.split(/[\\/]/);
  // 배열의 마지막 요소를 반환하여 파일 이름을 얻습니다.
  const fileName = pathParts[pathParts.length - 1];
  return decodeURIComponent(fileName);
}

  