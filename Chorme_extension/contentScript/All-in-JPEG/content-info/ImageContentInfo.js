import ImageInfo from "./ImageInfo.js"
export default class ImageContentInfo {
    constructor(imageContent) {
        this.contentInfoSize = 0;
        this.imageCount = 0;
        this.imageInfoList = [];

        this.XOI_MARKER_SIZE = 2;

        this.init(imageContent);
    }

    init(imageContent) {
        this.imageCount = imageContent.pictureCount;
        this.imageInfoList = this.fillImageInfoList(imageContent.pictureList);
        this.contentInfoSize = this.getLength();
    }

    fillImageInfoList(pictureList) {
        let offset = 0;
        let preSize = 0;
        let imageInfoList = [];
        
        for (let i = 0; i < pictureList.length; i++) {
            let imageInfo = new ImageInfo(pictureList[i]);
            
            if (i === 0) {
                preSize = imageInfo.imageDataSize;
            }

            if (i > 0) {
                offset += preSize;
                preSize = 2 + imageInfo.metaDataSize + imageInfo.imageDataSize;
            }

            imageInfo.offset = offset;
            imageInfoList.push(imageInfo);
        }

        return imageInfoList;
    }

    getLength() {
        let size = 0;
        for (let i = 0; i < this.imageInfoList.length; i++) {
            size += this.imageInfoList[i].getImageInfoSize();
        }
        size += 8;
        this.contentInfoSize = size;
        return size;
    }

    convertBinaryData(isBurst) {
        const buffer = new ArrayBuffer(this.getLength());
        const dataView = new DataView(buffer);

        dataView.setInt32(0, this.contentInfoSize);
        dataView.setInt32(4, this.imageCount);

        let offset = 8;
        for (let j = 0; j < this.imageCount; j++) {
            const imageInfo = this.imageInfoList[j];
            dataView.setInt32(offset, imageInfo.offset);
            dataView.setInt32(offset + 4, imageInfo.metaDataSize);
            dataView.setInt32(offset + 8, imageInfo.imageDataSize);
            dataView.setInt32(offset + 12, imageInfo.attribute);
            dataView.setInt32(offset + 16, imageInfo.embeddedDataSize);

            if (imageInfo.embeddedDataSize > 0) {
                for (let p = 0; p < imageInfo.embeddedDataSize / 4; p++) {
                    dataView.setInt32(offset + 20 + p * 4, imageInfo.embeddedData[p]);
                }
            }

            offset += 20 + imageInfo.embeddedDataSize;
        }
        return new Uint8Array(buffer);
    }

    getEndOffset() {
        const lastImageInfo = this.imageInfoList[this.imageInfoList.length-1];
        let extendImageDataSize = 0
        if(this.imageInfoList.size == 1){
            const lastImageInfo = this.imageInfoList[this.imageInfoList.length-1];
            extendImageDataSize = lastImageInfo.imageDataSize
        }else{
            const lastImageInfo = this.imageInfoList[this.imageInfoList.length-1];
            extendImageDataSize= this.XOI_MARKER_SIZE + lastImageInfo.metaDataSize + lastImageInfo.imageDataSize
        }
               //return lastImageInfo.offset + extendImageDataSize -1
        return lastImageInfo.offset + extendImageDataSize
    }
}
