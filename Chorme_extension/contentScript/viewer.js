import AiContainer from './All-in-JPEG/AiContainer.js';
import LoadResolver from './All-in-JPEG/LoadResolver.js';
import { chageMainImagetoSelectedImage, extractBasicMetadata, extractAiMetadata } from './All-in-JPEG/ImageView.js';

var aiContainer;
var loadResolver;

document.addEventListener("DOMContentLoaded", function(event) { // 웹 페이지의 DOM이 로드 시 실행
  console.log("DOM is loaded");
  // document가 로드되었을 때 ready 상태를 background.js에게 알립니다.
  chrome.runtime.sendMessage({ action: "ready" });
});


chrome.runtime.onMessage.addListener(function(message, sender, sendResponse) { // background.js 로부터 메시지를 수신
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

// content 메뉴 라디오버튼 처리
contentsRadioBtn.addEventListener("change", function() {
    if (this.checked) {
        contentMenuSpacer.style.visibility = "visible"
        metaDataMenuSpacer.style.visibility = "hidden"
        contentsMenuTab.style.display = "flex"
        meataDataMenuTab.style.display ="none"
    }
});

// metadata 메뉴 라디오버튼 처리
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
const app1MetaData = document.getElementById("meta-data-app1")
const imageContentMetaData = document.getElementById("meta-data-image-content")
const audioContentMetaData = document.getElementById("meta-data-audio-content")
const textContentMetaData = document.getElementById("meta-data-text-content")
const imageContentSection = document.getElementById("image-contents-section");


/**
 * 이미지 출력 (All-in JPEG or JPEG)
 * @param {*} imageUrl 출력할 이미지 url
 */
async function displayImage(imageUrl) { 
    const imageElement = document.getElementById("main_image")
    imageElement.src = imageUrl;
    getImageByteArrayFromURL(imageUrl)
    .then(async byteArray => {
      if (byteArray) {
        aiContainer = new AiContainer();
        loadResolver = new LoadResolver();
        await setBasicMetadataTab(byteArray, imageUrl)

         // All-in JPEG 파일인지 식별
        var isAllinJPEG = await loadResolver.isAllinJPEG(byteArray)
        if (isAllinJPEG) {

            document.getElementById("jpeg-type-display-div").innerHTML = "All-in JPEG 사진을 보고 있습니다."
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

            setAiMetadataTab(); // 메타데이터 추출
    
            // Auduio 있을 경우, 오디오 만듦.
            aiContainer.createAudio();
            audioContent.src = aiContainer.audioContent.blobUrl
      
            let isClicked = false;

            if (aiContainer.textContent.textCount !=0){
              textContent.innerHTML = aiContainer.textContent.textList[0].data
              textDisplayDiv.innerHTML = aiContainer.textContent.textList[0].data

              textContent.addEventListener('mouseenter', () => {
                textContent.style.backgroundColor = '#9177D0';
                textContent.style.color = 'white';
              });
              
              textContent.addEventListener('mouseleave', () => {
                if (!isClicked){
                  textContent.style.backgroundColor = '#F1F3F4'; // 기본 배경색으로 변경
                  textContent.style.color = 'black'; // 기본 글자색으로 변경
                }
              });


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
        }
        else { // 일반 JPEG 사진 출력
            document.getElementById("jpeg-type-display-div").innerHTML = "일반 JPEG 사진을 보고 있습니다."
            document.getElementById("contents-menu-tab").style.visibility = "hidden"
            document.getElementById("ai-meta-data-label").style.display = "none"
        }

    }
  });
}

/**
 * 이미지의 url로 부터 파일 얻기
 * @param {*} imageUrl 파일이름을 알아낼 이미지 url
 * @returns 파일 이름
 */
function getFileNameFromUrl(imageUrl) { 
  // 파일 경로를 "/" 또는 "\" 기준으로 분리하여 배열로 만듭니다.
  const pathParts = imageUrl.split(/[\\/]/);
  // 배열의 마지막 요소를 반환하여 파일 이름을 얻습니다.
  const fileName = pathParts[pathParts.length - 1];
  return decodeURIComponent(fileName);
}

/**
 * meta-data tab의 jpeg meta data setting
 * @param {*} byteArray 
 * @param {*} imageUrl 
 */
async function setBasicMetadataTab(byteArray, imageUrl) {
  let metadataString = ""
  const result = await extractBasicMetadata(byteArray)
  .then(metadata => {
    let make = metadata.make
    let model = metadata.model
    let captureTime = metadata.captureTime

    // 공백을 기준으로 문자열을 분할
    let parts = captureTime.split(" ");
    
    // 콜론(:)과 공백을 기준으로 문자열을 분할
    let dateParts = parts[0].split(/:|\s/);
    // 분할된 부분에서 년, 월, 일을 추출
    const year = dateParts[0];
    const month = dateParts[1];
    const day = dateParts[2];
    // 추출한 년, 월, 일을 합쳐서 원하는 형식으로 만듦
    const formattedDate = `${year}/${month}/${day}`;

    // 분할된 부분에서 시간 부분을 추출
    let time = parts[1];


    // meta data 정보 출력
    metadataString += `<p id="image-marker">App1 MetaData</p><hr>`
    metadataString += `<p id="attribute-marker">Name</p><p id="attribut-value">${getFileNameFromUrl(imageUrl)}</p><br></br>`
    metadataString += `<p id="attribute-marker">Date</p><p id="attribut-value">${formattedDate}</p><br></br>`
    metadataString += `<p id="attribute-marker">Time</p><p id="attribut-value">${time}</p><br></br>`
    metadataString += `<p id="attribute-marker">Make</p><p id="attribut-value">${make}</p><br>`
    metadataString += `<p id="attribute-marker">Model</p><p id="attribut-value">${model}</p><br>`


    app1MetaData.innerHTML = metadataString
  })
  .catch(error => {
  console.error('Error:', error);
  return error;
  })
  
}


/**
 * meta-data tab의 Ai Meata data setting
 */
function setAiMetadataTab(){ 
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