async function getImageByteArrayFromURL(imageURL) {
    try {
      const response = await fetch(imageURL);
      if (!response.ok) {
        throw new Error('Failed to fetch the image.');
      }
  
      const arrayBuffer = await response.arrayBuffer();
      const byteArray = new Uint8Array(arrayBuffer);
      return byteArray;
    } catch (error) {
      console.error('Error fetching the image: ', error);
      return null;
    }
}