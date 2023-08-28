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
        contentsMenuTab.style.display = "flex"
        meataDataMenuTab.style.display ="none"
    }
});

metaDataRadioBtn.addEventListener("change",function()
{
  if (this.checked) {
    metaDataMenuSpacer.style.visibility = "visible"
    contentMenuSpacer.style.visibility = "hidden"
    contentsMenuTab.style.display = "none"
    meataDataMenuTab.style.display = "block"
  }
});

const audioContent =  document.getElementById("audio-content");
const textContent = document.getElementById("text-content");
const textDisplayDiv = document.getElementById("text-display-div")
const imageContentMetaData = document.getElementById("meta-data-image-content")
const audioContentMetaData = document.getElementById("meta-data-audio-content")
const textContentMetaData = document.getElementById("meta-data-text-content")

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
     
        const SIZE = aiContainer.imageContent.pictureList.length
        document.getElementById("image-content-logo").innerText = `담긴 사진 ${SIZE} 장`
        for (let i = 0; i < SIZE; i++) {
          console.log(i);
          let pictureData = aiContainer.imageContent.pictureList[i]
          const img = document.createElement("img");
          img.src = aiContainer.imageContent.getBlobURL(pictureData) // 이미지 파일 경로 설정
          img.classList.add("sub_image");
       
          img.addEventListener('click', (e) =>{
            chageMainImagetoSelectedImage(e, aiContainer, i)
          })
          imageContentSection.appendChild(img);
        }
  
        // All-in JPEG 파일인지 식별 - boolean 값
        var isAllinJPEG = loadResolver.isAllinJPEG(byteArray)

        console.log(await getBasicMetadata());
        getAiMetadata();

       // Auduio 있을 경우, 오디오 만듦.
       aiContainer.createAudio();
       audioContent.src = aiContainer.audioContent.blobUrl

      
      //TODO: text가 있을 때, 없을 때에 따라 예외 처리 해야함
       let isClicked = false;
       textContent.innerHTML = aiContainer.textContent.textList[0].data
       textDisplayDiv.innerHTML = aiContainer.textContent.textList[0].data
       textContent.addEventListener('click', (e) =>{
        if (!isClicked) {
          textContent.style.backgroundColor = "#9177D0"
          textContent.style.color = "white"
          isClicked = true
          textDisplayDiv.style.visibility = "visible"
        }
        else {
          textContent.style.backgroundColor = "#F1F3F4"
          textContent.style.color = "black"
          isClicked = false
          textDisplayDiv.style.visibility = "hidden"
        }
      })
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
  const jsonString = extractAiMetadata(aiContainer)
  let metadataString = ""
  try {
    const data = JSON.parse(jsonString);
  
    // 이미지 정보 파싱
    const imageContentInfo = data.ImageContentInfo;
    let idx = 1;
    for (const imageInfo of imageContentInfo) {
      const size = imageInfo.Size;
      const offset = imageInfo.Offset;
      const attribute = imageInfo.Attribute;
      metadataString += `<p id="image-marker"># Image ${idx++}</p><hr>`
      metadataString += `<p id="attribute-marker">Image Size</p><p id="attribut-value">${size}</p><br>`
      metadataString += `<p id="attribute-marker">Offset</p><p id="attribut-value">${offset}</p><br>`
      metadataString += `<p id="attribute-marker">Attribute</p><p id="attribut-value">${attribute}</p><br></br>`
    }
    imageContentMetaData.innerHTML = metadataString

    // 텍스트 정보 파싱
    metadataString = ""
    const textContentInfo = data.textContentInfo;
    const textList = textContentInfo.TextList;
    for (const textInfo of textList) {
      const offset = textInfo.Offset;
      const text = textInfo.Text;
      metadataString += `<p id="attribute-marker">Text Offset</p><p id="attribut-value">${offset}</p><br>`
      metadataString += `<p id="attribute-marker">Text</p><p id="attribut-value">${text}</p>`
    }
    textContentMetaData.innerHTML = metadataString

    // 오디오 정보 파싱
    metadataString = ""
    const audioContentInfo = data.audioContentInfo;
    const audioOffset = audioContentInfo.Offset;
    const audioSize = audioContentInfo.Size;
    metadataString +=  `<p id="attribute-marker">Audio Offset</p><p id="attribut-value">${audioOffset}</p><br>`
    metadataString += `<p id="attribute-marker">Size</p><p id="attribut-value">${audioSize}</p>`
  } catch (error) {
    console.error('Error parsing JSON:', error);
  }
  audioContentMetaData.innerHTML = metadataString
}


const maingImage = document.getElementById("main_image")

maingImage.onload = function() {
  // Get image dimensions
  const width = maingImage.width;
  const height = maingImage.height;

  // Get image EXIF data
  EXIF.getData(maingImage, function() {
    const dateTime = EXIF.getTag(this, "DateTimeOriginal");
    const rotation = EXIF.getTag(this, "Orientation");
    const latitude = EXIF.getTag(this, "GPSLatitude");
    const longitude = EXIF.getTag(this, "GPSLongitude");
  });

};