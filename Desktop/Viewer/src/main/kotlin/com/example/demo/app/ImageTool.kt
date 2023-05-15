package com.example.demo.app

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.MetadataException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.jpeg.JpegDirectory
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream


class ImageTool {

 fun getOrientation(bytes : ByteArray) : Int {
     val imageFile = File("file.jpg")
     imageFile.writeBytes(bytes)

     // 2. 원본 파일의 Orientation 정보를 읽는다.
     var orientation = 1 // 회전정보, 1. 0도, 3. 180도, 6. 270도, 8. 90도 회전한 정보
     val metadata: Metadata // 이미지 메타 데이터 객체
     val directory: Directory? // 이미지의 Exif 데이터를 읽기 위한 객체

     try {
         metadata = ImageMetadataReader.readMetadata(imageFile)
         directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
         if (directory != null) {
             orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION) // 회전정보
             when (orientation) {
                 1 -> return 0
                 3 -> return 180
                 6 -> return 270
                 8 -> return 90
             }
         }

     } catch (e: ImageProcessingException) {
         e.printStackTrace()
     } catch (e: MetadataException) {
         e.printStackTrace()
     } catch (e: IOException) {
         e.printStackTrace()
     }
     return 0
 }
    fun rotaionImage(image : Image, rotation : Int) : Image{
        var angle : Int = 360 - rotation
        var bufferedImage = SwingFXUtils.fromFXImage(image, null)
        var newBufferedImage = rotateImageClockwise(bufferedImage, angle.toDouble())
        var newImage = SwingFXUtils.toFXImage(newBufferedImage, null)
       return newImage
    }

    fun rotateImageClockwise(image: BufferedImage, angle: Double): BufferedImage {
        val radians = Math.toRadians(angle)
        val rotatedWidth = Math.abs(Math.sin(radians) * image.height) + Math.abs(Math.cos(radians) * image.width)
        val rotatedHeight = Math.abs(Math.sin(radians) * image.width) + Math.abs(Math.cos(radians) * image.height)
        val rotatedImage = BufferedImage(rotatedWidth.toInt(), rotatedHeight.toInt(), image.type)
        val graphics2D = rotatedImage.createGraphics()
        graphics2D.translate((rotatedWidth - image.width) / 2.0, (rotatedHeight - image.height) / 2.0)
        graphics2D.rotate(radians, image.width / 2.0, image.height / 2.0)
        graphics2D.drawRenderedImage(image, null)
        graphics2D.dispose()
        return rotatedImage
    }
}
