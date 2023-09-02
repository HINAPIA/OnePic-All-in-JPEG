import ContentAttribute from './content/contentType.js';
// import EXIF from 'exif-js';
// import exifr from 'exifr';
// import dayjs from 'dayjs';

/*
* 사용자가 클릭한 사진으로 메인 이미지를 변경
*/
export function chageMainImagetoSelectedImage (event, aiContainer , index){
    let selectedPicture;
    if(aiContainer.imageContent.pictureList.length > index){
        selectedPicture = aiContainer.imageContent.pictureList[index];
    }
    else{
        selectedPicture = aiContainer.imageContent.pictureList[0];
    }
    var newSrc =  aiContainer.imageContent.getBlobURL(selectedPicture);
   
    const mainImageElement = document.getElementById("main_image");
    mainImageElement.src =  newSrc;
}


/*
* JPEG의 일반 메타데이터를 추출하여 Json 형식으로 리턴
*/
export async function extractBasicMetadata(imageBytes) {
    try {
      const metadata = await getMetadataFromImageBytes(imageBytes);
      console.log('Metadata:', metadata);
      return metadata;
    } catch (error) {
      console.error('Error:', error);
      throw error; // 이 부분에서 예외를 다시 던져서 호출자에게 전달합니다.
    }
  }
  

/*
* All-in JPEG 전용 메타데이터를 추출하여 Json 형식으로 리턴
*/
export function extractAiMetadata(aiContainer){
    aiContainer.settingHeaderInfo();
    var header = aiContainer.header;

    // 데이터를 담을 JavaScript 객체 생성
    var dataObject = {};
    var imageMeta = getImageContentMetadata(header.imageContentInfo)
    var textMeta = getTextContentMetadata(header.textContentInfo)
    var audioMeta = getAudioContentMetadata(header.audioContentInfo)

    dataObject.ImageContentInfo = imageMeta;
    dataObject.textContentInfo = textMeta;
    dataObject.audioContentInfo = audioMeta;
    var jsonResult = JSON.stringify(dataObject);
    console.log(`${jsonResult}`);
    return jsonResult
}

function getMetadataFromImageBytes(imageBytes) {
    return new Promise((resolve, reject) => {
        const imageFile = new Blob([imageBytes]);

        EXIF.getData(imageFile, function() {
            const exifData = EXIF.getAllTags(this);

            const metadata = {
                captureTime: exifData.DateTimeOriginal || "2023-5-25 23:25",
                latitude: exifData.GPSLatitude || "37/1 31/1",
                longitude: exifData.GPSLongitude || "127/1 6/1",
                width: exifData.PixelXDimension || 0,
                height: exifData.PixelYDimension || 0,
                make: exifData.Make,
                model: exifData.Model,
                focalLength: exifData.FocalLength,
                exposureTime: exifData.ExposureTime,
                apertureValue: exifData.ApertureValue,
                // ... 다른 정보들도 필요에 따라 추가해줄 수 있습니다.
            };

            resolve(metadata);
        });
    });
}

/*
* All-in JPEG 전용 메타데이터를 중 Image 메타 데이터를 추출하여 Json 형식으로 리턴
*/
function getImageContentMetadata(imageContentInfo){
    var imageInfoList = imageContentInfo.imageInfoList;
    var result = {};
    var resultList = [];

    result.Count = imageContentInfo.imageCount;
    imageInfoList.forEach(imageInfo => {
        var dataObject = {};
        // Size 데이터 추가
        var sizeValue = ((imageInfo.imageDataSize).toFixed(2) / 1000).toFixed(2);
        dataObject.Size = `${sizeValue}kb`;

        // Offset 데이터 추가
        dataObject.Offset = ((imageInfo.offset).toFixed(2) / 1000).toFixed(2);

        // Attribute 데이터 추가
        var attribute = ContentAttribute.fromCode(imageInfo.attribute);
        dataObject.Attribute = attribute.toString();

         // 현재 imageInfo의 데이터를 dataObject에 추가
        resultList.push(dataObject);
    });
    result.ImageList = resultList;
    // JSON 형식으로 변환하여 리턴
    return resultList;
}

/*
* All-in JPEG 전용 메타데이터를 중 Text 메타 데이터를 추출하여 Json 형식으로 리턴
*/
function getTextContentMetadata(textContentInfo){
    var textInfoList = textContentInfo.textInfoList;
    var result = {};
    var resultList = [];

    // count 데이터 추가
    result.Count = textContentInfo.textCount
     // textInfoList[]  추가
    textInfoList.forEach(textInfo =>{
        var dataObject = {};
        // offset 데이터 추가
        dataObject.Offset = ((textInfo.offset).toFixed(2) / 1000).toFixed(2);
        // text 데이터 추가
        dataObject.Text = textInfo.data;
        resultList.push(dataObject);
    })
    result.TextList = resultList;
    return result
}

/*
* All-in JPEG 전용 메타데이터를 중 Audio 메타 데이터를 추출하여 Json 형식으로 리턴
*/
function getAudioContentMetadata(audioContentInfo){
    var result = {};
    // offset 데이터 추가
    result.Offset = ((audioContentInfo.dataStartOffset).toFixed(2) / 1000).toFixed(2);
    // Size 데이터 추가
    var sizeValue = ((audioContentInfo.datasize).toFixed(2) / 1000).toFixed(2);
    result.Size = `${sizeValue}kb`;
    return result
}

