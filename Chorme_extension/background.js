
var imageUrl = "" // 열고자 하는 파일 url
var tabID = "" // 현재 새롭게 열린 tabID (확장팩)

chrome.webNavigation.onBeforeNavigate.addListener(function(details) {  // 페이지 네비게이션이 시작되기 전에 호출되는 콜백 함수
  if (details.url && details.url.startsWith("file://") && (details.url.toLowerCase().endsWith(".jpg") || details.url.toLowerCase().endsWith(".jpeg") )) { // file:// 프로토콜로 열린 파일을 감지했을 때의 처리
    imageUrl = details.url
    chrome.tabs.create({ url: chrome.runtime.getURL("viewer.html")}, function(newTab){
      console.log("Chrome tab created: tabID - "+newTab.id)
      tabID = newTab.id
    });
  }
});


chrome.runtime.onMessage.addListener(function(message, sender, sendResponse) { // viewer.js에서 보낸 준비 완료 메시지를 받는 부분
  if (message.action === "ready") {
    console.log("Message send : displayImage")
    chrome.tabs.sendMessage(tabID, { action: "displayImage", url: imageUrl },null,null)
  }
});


