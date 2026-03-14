/**
 * IBN SINA Inventory Management - UPDATED SCRIPT
 * Tracks User ID and User Name on every update.
 */

var MAIN_SS_ID = '1UKCJz6vT_N9L-uhhGDVtVVJOVE5CVurzZXJZo167vu8';
var MASTER_SS_ID = '1pfse25kJEycNL9FWPkXS0oSBcWgQfL2ApWVcChMZ09k';

function doGet(e) {
  var action = e.parameter.action;

  try {
    var ss = SpreadsheetApp.openById(MAIN_SS_ID);
    var appSheet = ss.getSheetByName('MOBIL APPS');

    // 1. Fetch Data
    if (action == "getJson") {
      var lastRow = appSheet.getLastRow();
      if (lastRow < 2) return ContentService.createTextOutput(JSON.stringify([])).setMimeType(ContentService.MimeType.JSON);
      var data = appSheet.getRange(2, 1, lastRow - 1, 13).getValues();
      var list = [];
      for (var i = 0; i < data.length; i++) {
        if (data[i][1] != "") {
          var rawStatus = String(data[i][11]).trim().toLowerCase();
          var finalStatus = (rawStatus === "checked") ? "Checked" : "Unchecked";
          list.push({
            "sl": data[i][12], "category": data[i][0], "code": data[i][1],        
            "productName": data[i][2], "packSize": data[i][3], "totalQty": data[i][4],
            "cartonSize": data[i][7], "shortQty": data[i][8], "excessQty": data[i][9],
            "remark": data[i][10], "status": finalStatus
          });
        }
      }
      return ContentService.createTextOutput(JSON.stringify(list)).setMimeType(ContentService.MimeType.JSON);
    }

    // 2. Update Stock with User Tracking (FIXED)
    if (action == "updateStock") {
      var code = String(e.parameter.code).trim().toLowerCase();
      var uId = e.parameter.userId;     // অ্যাপ থেকে পাঠানো ইউজার আইডি
      var uName = e.parameter.userName; // অ্যাপ থেকে পাঠানো ইউজার নাম
      
      var data = appSheet.getDataRange().getValues();
      for (var i = 1; i < data.length; i++) {
        if (String(data[i][1]).trim().toLowerCase() === code) {
          appSheet.getRange(i + 1, 9).setValue(e.parameter.shortQty);  // Column I
          appSheet.getRange(i + 1, 10).setValue(e.parameter.excessQty); // Column J
          appSheet.getRange(i + 1, 11).setValue(e.parameter.remark);    // Column K
          appSheet.getRange(i + 1, 12).setValue(e.parameter.status == "Checked" ? "Checked" : ""); // Column L
          
          // অতিরিক্ত ট্র্যাক কলাম (N এবং O)
          appSheet.getRange(i + 1, 14).setValue(uId);   // Column N (User ID)
          appSheet.getRange(i + 1, 15).setValue(uName); // Column O (User Name)
          
          return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
        }
      }
      return ContentService.createTextOutput("Not Found").setMimeType(ContentService.MimeType.TEXT);
    }

    // 3. Reset All
    if (action == "resetAll") {
      var lastRow = appSheet.getLastRow();
      if (lastRow >= 2) {
        appSheet.getRange(2, 12, lastRow - 1, 1).clearContent(); // L
        appSheet.getRange(2, 9, lastRow - 1, 3).clearContent();  // I, J, K
        appSheet.getRange(2, 14, lastRow - 1, 2).clearContent(); // N, O (User info ক্লিয়ার)
      }
      return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
    }

    // 4. Update Carton Size
    if (action == "updateCartonSize") {
      var searchCode = String(e.parameter.code).trim().toLowerCase();
      var newSize = e.parameter.cartonSize;
      var masterSS = SpreadsheetApp.openById(MASTER_SS_ID);
      var masterSheet = masterSS.getSheetByName('PHARMA');
      var masterData = masterSheet.getDataRange().getValues();
      for (var i = 0; i < masterData.length; i++) {
        if (String(masterData[i][1]).trim().toLowerCase() === searchCode) {
          masterSheet.getRange(i + 1, 5).setValue(newSize);
          return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
        }
      }
      return ContentService.createTextOutput("Error: Code not found").setMimeType(ContentService.MimeType.TEXT);
    }

    // 5. Web View
    const template = HtmlService.createTemplateFromFile('Index');
    return template.evaluate().setTitle('Stock View Auto').setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);

  } catch (err) {
    return ContentService.createTextOutput("System Error: " + err.message).setMimeType(ContentService.MimeType.TEXT);
  }
}
