/**
 * IBN SINA Inventory Management - MASTER SCRIPT
 * Android App Uses: "MOBIL APPS" (Row 2, Status Column L, Tracking N & O)
 * Web View Uses: "ALL STOCK"
 * Carton Size Update: Targets sheet ID 1pfse... Tab: PHARMA
 */

var MAIN_SS_ID = '1UKCJz6vT_N9L-uhhGDVtVVJOVE5CVurzZXJZo167vu8';
var MASTER_SS_ID = '1pfse25kJEycNL9FWPkXS0oSBcWgQfL2ApWVcChMZ09k'; 

function doGet(e) {
  var action = e.parameter.action;
  
  try {
    var ss = SpreadsheetApp.openById(MAIN_SS_ID);
    var appSheet = ss.getSheetByName('MOBIL APPS');

    // 1. Android: Fetch Data (Starting from Row 2)
    if (action == "getJson") {
      var lastRow = appSheet.getLastRow();
      if (lastRow < 2) return ContentService.createTextOutput(JSON.stringify([])).setMimeType(ContentService.MimeType.JSON);
      
      var data = appSheet.getRange(2, 1, lastRow - 1, 13).getValues(); 
      var list = [];
      for (var i = 0; i < data.length; i++) {
        if (data[i][1] != "") {
          // Robust status check (handles spaces and case sensitivity)
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

    // 2. Android: Update Stock with User ID & Name Tracking
    if (action == "updateStock") {
      var code = String(e.parameter.code).trim().toLowerCase();
      var uId = e.parameter.userId;     
      var uName = e.parameter.userName; 
      
      var data = appSheet.getDataRange().getValues();
      for (var i = 1; i < data.length; i++) {
        if (String(data[i][1]).trim().toLowerCase() === code) {
          appSheet.getRange(i + 1, 9).setValue(e.parameter.shortQty);  // I
          appSheet.getRange(i + 1, 10).setValue(e.parameter.excessQty); // J
          appSheet.getRange(i + 1, 11).setValue(e.parameter.remark);    // K
          appSheet.getRange(i + 1, 12).setValue(e.parameter.status == "Checked" ? "Checked" : ""); // L
          
          // User Tracking (Columns N & O)
          appSheet.getRange(i + 1, 14).setValue(uId);   // N
          appSheet.getRange(i + 1, 15).setValue(uName); // O
          
          return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
        }
      }
      return ContentService.createTextOutput("Not Found").setMimeType(ContentService.MimeType.TEXT);
    }

    // 3. Android: Reset All Data
    if (action == "resetAll") {
      var lastRow = appSheet.getLastRow();
      if (lastRow >= 2) {
        appSheet.getRange(2, 12, lastRow - 1, 1).clearContent(); // Clear Status (L)
        appSheet.getRange(2, 9, lastRow - 1, 3).clearContent();  // Clear I, J, K
        appSheet.getRange(2, 14, lastRow - 1, 2).clearContent(); // Clear N, O
      }
      return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
    }

    // 4. Android: Update Carton Size in Target Sheet (Tab: PHARMA)
    if (action == "updateCartonSize") {
      var searchCode = String(e.parameter.code).trim().toLowerCase();
      var newSize = e.parameter.cartonSize;
      var masterSS = SpreadsheetApp.openById(MASTER_SS_ID);
      var masterSheet = masterSS.getSheetByName('PHARMA');
      
      if (!masterSheet) return ContentService.createTextOutput("Error: PHARMA tab not found").setMimeType(ContentService.MimeType.TEXT);
      
      var masterData = masterSheet.getDataRange().getValues();
      for (var i = 0; i < masterData.length; i++) {
        var sheetCode = String(masterData[i][1]).trim().toLowerCase();
        if (sheetCode === searchCode) {
          masterSheet.getRange(i + 1, 5).setValue(newSize); // Column E
          return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
        }
      }
      return ContentService.createTextOutput("Error: Code not found").setMimeType(ContentService.MimeType.TEXT);
    }

    // 5. Android: Login & AutoFill
    if (action == "login" || action == "getAutoFill") {
      var searchId = e.parameter.userId;
      var pass = e.parameter.password;
      var credSheet = ss.getSheetByName('Credentials');
      var credData = credSheet.getDataRange().getValues();
      for (var i = 1; i < credData.length; i++) {
        if (credData[i][0] == searchId) {
          if (action == "login" && credData[i][3] != pass) continue;
          return ContentService.createTextOutput(JSON.stringify({
            "success": true, "name": credData[i][1], "designation": credData[i][2]
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"success": false})).setMimeType(ContentService.MimeType.JSON);
    }

    // 6. Web App: Serve HTML (Uses "ALL STOCK" internally via template)
    const template = HtmlService.createTemplateFromFile('Index');
    template.userId = e.parameter.userId || '';
    template.userName = e.parameter.username || '';
    return template.evaluate()
      .setTitle('Stock View Auto')
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
      .setFaviconUrl('https://images.seeklogo.com/logo-png/53/1/ibn-sina-medical-logo-png_seeklogo-536126.png');

  } catch (err) {
    return ContentService.createTextOutput("System Error: " + err.message).setMimeType(ContentService.MimeType.TEXT);
  }
}
