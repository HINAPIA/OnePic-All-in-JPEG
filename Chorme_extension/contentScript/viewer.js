import AiContainer from './All-in-JPEG/AiContainer.js';
import LoadResolver from './All-in-JPEG/LoadResolver.js';
import { chageMainImagetoSelectedImage, extractBasicMetadata, extractAiMetadata } from './All-in-JPEG/ImageView.js';

var aiContainer;
var loadResolver;

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


/* nav menu 선택 처리 */
const contentsRadioBtn = document.getElementById("contents-menu-btn"); // contents-menu radio btn
const metaDataRadioBtn = document.getElementById("meta-data-menu-btn"); // meta-data-menu radio btn
const contentMenuSpacer = document.getElementById("contents-menu-spacer");
const metaDataMenuSpacer = document.getElementById("meta-data-menu-spacer");
const contentsMenuTab = document.getElementById("contents-menu-tab");
const meataDataMenuTab = document.getElementById("meta-data-menu-tab");

// 첫 번째 라디오 버튼에 이벤트 리스너를 등록합니다.
contentsRadioBtn.addEventListener("change", function() {
    if (this.checked) {
        contentMenuSpacer.style.visibility = "visible"
        metaDataMenuSpacer.style.visibility = "hidden"
        contentsMenuTab.style.display = "block"
        meataDataMenuTab.style.display ="none"
    }
});

metaDataRadioBtn.addEventListener("change",function()
{
  if (this.checked) {
    metaDataMenuSpacer.style.visibility = "visible"
    contentMenuSpacer.style.visibility = "hidden"
    contentsMenuTab.style.display = "none"
    meataDataMenuTab.style.display ="block"
  }
});


const imageContentSection = document.getElementById("image-contents-section");

async function displayImage(imageUrl) { // 이미지를 보여주는 함수를 정의합니다.
    const imageElement = document.getElementById("main_image")
    imageElement.src = imageUrl;
    // document.getElementById("file-name").innerText = getFileNameFromUrl(imageUrl)
    getImageByteArrayFromURL(imageUrl)
    .then(async byteArray => {
      if (byteArray) {
        console.log('Image Byte Array:', byteArray);
        aiContainer = new AiContainer();
        loadResolver = new LoadResolver();
        await loadResolver.createAiContainer(aiContainer, byteArray);
        // while(true){
        //   await new Promise(resolve => setTimeout(resolve, 1000));
        //   break;
        // }
     
        for (let i = 0; i < aiContainer.imageContent.pictureList.length; i++) {
          console.log(i);
          let pictureData = aiContainer.imageContent.pictureList[i]
          const img = document.createElement("img");
          img.src = aiContainer.imageContent.getBlobURL(pictureData);; // 이미지 파일 경로 설정
          img.classList.add("sub_image");
       
          img.addEventListener('click', (e) =>{
            chageMainImagetoSelectedImage(e, aiContainer, i)
          })
          imageContentSection.appendChild(img);
        }
      
        // let testPicture = aiContainer.imageContent.pictureList[1]
        // const testImageElement = document.getElementById("sub_image1");
        // testImageElement.src = aiContainer.imageContent.getBlobURL(testPicture);

        // testPicture = aiContainer.imageContent.pictureList[2]
        // const testImageElement2 = document.getElementById("sub_image2");
        // testImageElement2.src = aiContainer.imageContent.getBlobURL(testPicture);
      
        //addSubImageEvent();
        console.log(await getBasicMetadata());
        getAiMetadata();

        aiContainer.playAudio();
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

// sub_image 클래스의 element를 클릭하면 메인 이미지 변경 리스너 추가
function addSubImageEvent(){
  var subImageElements = document.querySelectorAll('.sub_image');
  subImageElements.forEach((element, i)=>{
    element.addEventListener('click', (e) =>{
      chageMainImagetoSelectedImage(e, aiContainer, i)
    });
  });
}

async function  getBasicMetadata(){
  var firstImageBytes =
  aiContainer.imageContent.getJpegBytes(aiContainer.imageContent.pictureList[0])

  console.log(await extractBasicMetadata(firstImageBytes));
 // console.log(jsonData);
}

function getAiMetadata(){
  extractAiMetadata(aiContainer)
}