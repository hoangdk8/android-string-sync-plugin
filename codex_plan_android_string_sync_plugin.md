# Plan chi tiết để dùng Codex tạo plugin Android Studio/IntelliJ cho String Sync từ Google Sheet

## 1) Mục tiêu

Xây một plugin dùng lại được cho nhiều project Android/Android Studio, thay cho cách copy task `syncStrings` vào từng `build.gradle.kts`.

Plugin này phải hỗ trợ đúng workflow bạn mô tả:

1. Có ô để dán Google Apps Script URL dạng `/exec`.
2. Detect được các module trong project để chọn nơi add string.
3. Detect được các ngôn ngữ đã tồn tại bằng file `strings.xml` để auto-focus/auto-select.
4. Cho chọn target languages cần add.
5. Có 2 mode chính:
   - **Add vào tất cả key được chỉ định**
   - **Chỉ add các key chưa có**
6. Có ô dán danh sách key cần add.
7. Có thể dùng lại cho nhiều project khác nhau.

Hiện tại tool Gradle của bạn đã có logic nền rất rõ: lấy JSON từ Google Apps Script URL, hỗ trợ `mode`, hỗ trợ `keys`, validate key, validate XML, validate placeholder, và tạo/cập nhật `strings.xml` theo locale. Điều này là nền tốt để chuyển thành plugin. fileciteturn2file0 fileciteturn2file1

---

## 2) Những gì đã có sẵn từ tool hiện tại và nên tái sử dụng

Theo file bạn đã upload, tool hiện tại đang có các hành vi sau:

- Đọc dữ liệu từ Google Apps Script endpoint `/exec`. fileciteturn2file0
- Hỗ trợ các mode `overwrite`, `append`, `merge`. fileciteturn2file0
- Hỗ trợ chọn 1 tập key qua `-Pkeys=...`. fileciteturn2file0
- Dò English base locale để so placeholder. fileciteturn2file1
- Validate key bằng regex `^[a-z0-9_]+$`. fileciteturn2file1
- Chặn XML injection kiểu `<string` hoặc `</resources>`. fileciteturn2file1
- Escape XML.
- So khớp placeholder `%s`, `%d`, `%1$s` giữa bản English và ngôn ngữ khác. fileciteturn2file1
- Tạo hoặc cập nhật `strings.xml` trong `values`, `values-xx`. fileciteturn2file0
- Guide hiện tại cũng mô tả rõ cấu trúc sheet: cột `KEY`, `Max-Length`, rồi các cột locale như `en-US`, `vi-VN`, `de-DE`, ...; và mô tả dùng `overwrite`, `append`, `merge`. fileciteturn2file0

### Ghi chú quan trọng cần sửa khi chuyển sang plugin

Trong Gradle task hiện tại, locale folder đang được tạo bằng:

- lấy `localeFull.substringBefore("-")`
- rồi map thành `values` hoặc `values-<language>`

Tức là các locale như `pt-BR`, `es-MX`, `zh-CN` sẽ bị rút thành `pt`, `es`, `zh`. Cách này đơn giản nhưng có thể làm mất region-specific translation. Plugin mới nên xử lý mapping locale cẩn thận hơn, thay vì luôn cắt sau dấu `-`. Điều này đặc biệt quan trọng vì sheet mẫu của bạn đang dùng nhiều mã locale theo dạng `hi-IN`, `pt-BR`, `es-MX`, `de-DE`, `ar-SA`, `fr-FR`... như trong ảnh và guide. fileciteturn2file1 fileciteturn1file8

---

## 3) Chọn dạng plugin

### Khuyến nghị

Làm **IntelliJ Platform plugin** chạy trong **Android Studio**.

JetBrains hiện khuyến nghị tạo plugin bằng **Gradle-based project** từ New Project Wizard hoặc template chính thức. citeturn359311search1turn359311search3

Android Studio chạy trên IntelliJ Platform. Nếu plugin chỉ dùng API chung của IntelliJ Platform thì có thể dựa vào `com.intellij.modules.platform`; nếu dùng Android Studio-specific APIs thì phải khai báo phụ thuộc phù hợp. JetBrains ghi rõ:

- dùng API Android plugin thì khai báo `org.jetbrains.android`
- dùng Android Studio-specific features thì khai báo thêm `com.intellij.modules.androidstudio` citeturn359311search0turn359311search10

### Quyết định thực tế cho bài toán này

Plugin của bạn cần:

- đọc module trong project
- tìm resource directories Android
- tìm `strings.xml`
- ghi file XML
- có UI dialog/settings

Cho bản đầu, nên target Android Studio và khai báo:

- `com.intellij.modules.platform`
- `org.jetbrains.android`
- chỉ thêm `com.intellij.modules.androidstudio` nếu thật sự cần API đặc thù của Android Studio

Làm vậy sẽ bớt khóa chặt plugin vào một version Android Studio cụ thể.

---

## 4) UX đề xuất cho plugin

### 4.1 Entry point

Thêm 1 action trong menu:

- `Tools > String Sync > Sync from Google Sheet`

Khi bấm sẽ mở 1 dialog chính.

JetBrains khuyến nghị dùng `DialogWrapper` cho modal dialog trong plugin. Nó đã hỗ trợ layout nút, validation, nhớ size, error text và flow chuẩn của IDE. citeturn577939search1

### 4.2 Màn hình chính

Dựa trên workflow và ảnh bạn gửi, dialog nên có các phần sau:

#### A. Sheet Source

- Text field: `Google Apps Script URL`
- Nút: `Test Connection`
- Nút: `Load Languages`

Validation:

- bắt buộc là URL
- bắt buộc kết thúc bằng `/exec`
- nếu lỗi network hoặc JSON lỗi thì show message rõ ràng

#### B. Source Modules

Bảng/list checkbox tương tự ảnh của bạn:

- Auto-detect modules
- Add...
- Deselect all
- Remove all

Mỗi item module nên hiển thị:

- tên module
- đường dẫn chính
- số resource locale đã có
- danh sách locale detect được, ví dụ: `ar, bn, de, es, fil ... (+10)`

Ảnh chụp hiện tại của bạn cũng đang đi theo UX này: danh sách module có trạng thái existing translations và cho chọn nhiều module. fileciteturn1file9

#### C. Language Selection

Panel tương tự ảnh:

- Source language: mặc định `English (en)` hoặc locale English detect được từ JSON
- Target Languages: list checkbox
- Search box
- Checkbox `Show ISO codes`
- Nút `Select All`
- `Deselect All`
- `Detect Existing`

Khi load xong JSON từ sheet, plugin sẽ có list locale từ remote.

Khi detect module/resource hiện có, plugin sẽ auto-focus hoặc auto-select các locale đã tồn tại trong project. Ảnh hiện tại của bạn cho thấy màn hình này là hợp lý và trực quan. fileciteturn1file7

#### D. Sync Mode

Radio buttons:

- `Add / Update selected keys in selected languages`
- `Add only missing keys`
- `Preview only (no write)`

Lưu ý: bản Gradle cũ đang có `overwrite`, `append`, `merge`; nhưng theo workflow bạn mô tả, UI cuối nên đơn giản thành 2 mode chính cho người dùng. `Preview only` là mode phụ để an toàn hơn.

Map đề xuất:

- `Add / Update selected keys` ~= `merge`
- `Add only missing keys` ~= `append`

Chỉ nên thêm `Replace entire file` ở bản sau nếu thật sự cần.

#### E. Keys Input

- Text area lớn để paste keys
- Hỗ trợ cả:
  - comma-separated
  - newline-separated
  - mixed separators
- Có nút `Load all keys from sheet`
- Có nút `Detect missing keys in selected modules`

Ví dụ input hợp lệ:

```text
msg_title_onboarding
msg_des_onboarding
msg_i_am_s
```

hoặc

```text
msg_title_onboarding,msg_des_onboarding,msg_i_am_s
```

#### F. Bottom actions

- `Preview Changes`
- `Apply`
- `Cancel`

Nếu muốn giống tool cũ hơn, label nút có thể là:

- `Add to all selected keys`
- `Add only missing keys`

Ảnh cuối của bạn đang có 2 CTA kiểu này cho tool dịch. fileciteturn1file6

---

## 5) Kiến trúc plugin nên yêu cầu Codex tạo

Đừng để Codex viết tất cả trong 1 file. Bắt Codex tách thành các lớp rõ ràng.

### 5.1 Package structure đề xuất

```text
com.yourcompany.stringsync
  actions/
    OpenStringSyncDialogAction.kt
  ui/
    StringSyncDialog.kt
    ModuleSelectionPanel.kt
    LanguageSelectionPanel.kt
    KeysInputPanel.kt
    PreviewChangesDialog.kt
  services/
    StringSyncService.kt
    StringSyncSettingsService.kt
    AndroidModuleScanner.kt
    ResourceScanner.kt
    SheetApiClient.kt
    StringXmlWriter.kt
    StringXmlParser.kt
  model/
    SheetLocale.kt
    SheetRow.kt
    SheetPayload.kt
    ModuleTarget.kt
    LanguageTarget.kt
    SyncMode.kt
    SyncRequest.kt
    SyncResult.kt
    FileChangePreview.kt
  util/
    LocaleMapper.kt
    PlaceholderValidator.kt
    XmlEscaper.kt
    KeyParser.kt
    NotificationUtils.kt
    BackgroundTaskUtils.kt
  resources/
    META-INF/plugin.xml
```

### 5.2 Vai trò từng lớp

#### `OpenStringSyncDialogAction`

- entry point từ menu
- lấy `Project`
- mở `StringSyncDialog`

#### `StringSyncDialog`

- container chính cho UI
- bind dữ liệu từ các panel
- validate input trước khi chạy
- gọi service chạy preview/apply

#### `StringSyncSettingsService`

- lưu config dùng lại giữa nhiều project hoặc nhiều lần dùng
- nên lưu:
  - last used script URL
  - last selected source language
  - last selected target languages
  - last selected mode
  - last entered keys
  - tùy chọn auto-select existing languages

JetBrains khuyến nghị persist state bằng `PersistentStateComponent`, thường đặt trong một service. citeturn577939search0turn577939search2

#### `SheetApiClient`

- gọi HTTP GET tới Apps Script URL
- parse JSON
- validate payload
- expose model nội bộ cho plugin

#### `AndroidModuleScanner`

- scan module trong project
- lọc module có khả năng là Android/resource module
- tìm content roots và res directories

#### `ResourceScanner`

- scan `src/main/res`, `src/debug/res`, `src/flavor/res` nếu cần
- detect file `values/strings.xml`, `values-vi/strings.xml`, ...
- trả về danh sách locale hiện có trong từng module

#### `StringXmlParser`

- parse existing `strings.xml`
- đọc key hiện có
- có thể giữ comment/order cơ bản nếu bạn muốn preview tốt hơn

#### `StringXmlWriter`

- nhận dữ liệu đã validate
- tạo change set
- ghi file
- không nên ghi trực tiếp từ UI thread

#### `PlaceholderValidator`

- tái dùng logic hiện tại từ tool Gradle
- so placeholder giữa base English và target locale
- báo lỗi cụ thể theo key/module/language

---

## 6) Contract dữ liệu giữa plugin và Google Apps Script

### 6.1 Contract hiện tại

Tool cũ đang parse JSON theo dạng:

```kotlin
Map<String, Map<String, String>>
```

Tức là:

```json
{
  "en-US": {
    "msg_title_onboarding": "Which team do...",
    "msg_des_onboarding": "Don't worry..."
  },
  "vi-VN": {
    "msg_title_onboarding": "Bạn thuộc về...",
    "msg_des_onboarding": "Đừng lo nhé..."
  }
}
```

Đây là contract đơn giản, phù hợp để plugin bản đầu tái sử dụng nguyên xi. fileciteturn2file1

### 6.2 Contract nên nâng cấp trong bản 2

Nếu bạn muốn preview tốt hơn, API nên trả thêm metadata:

```json
{
  "baseLocale": "en-US",
  "locales": ["en-US", "vi-VN", "pt-BR"],
  "rows": [
    {
      "key": "msg_title_onboarding",
      "maxLength": "",
      "translations": {
        "en-US": "Which team do...",
        "vi-VN": "Bạn thuộc về...",
        "pt-BR": "A qual equipe..."
      }
    }
  ]
}
```

Bản đầu chưa cần đổi nếu bạn muốn ship nhanh.

---

## 7) Luồng xử lý chi tiết plugin phải làm

### Phase A. Load dữ liệu từ sheet

1. User paste Apps Script URL.
2. User bấm `Load Languages`.
3. Plugin gọi API.
4. Plugin parse JSON.
5. Plugin detect English/base locale.
6. Plugin build danh sách locale khả dụng.
7. Plugin render list target languages.

Validation phải có:

- URL rỗng
- URL không kết thúc `/exec`
- timeout
- HTTP lỗi
- JSON parse lỗi
- không có English locale

Guide bạn upload cũng ghi rõ URL phải là web app `/exec`, và flow hiện tại dùng Google Apps Script làm API. fileciteturn2file0

### Phase B. Detect module trong project

1. Scan tất cả IntelliJ modules.
2. Lọc các module có ít nhất 1 resource root Android.
3. Với mỗi module, detect:
   - res path
   - values folders
   - locales hiện có
   - số lượng string files
4. Hiển thị vào panel module selection.

### Phase C. Detect existing languages

1. Với các module đang được chọn, scan các folder:
   - `values`
   - `values-vi`
   - `values-es`
   - `values-b+sr+Latn` nếu về sau hỗ trợ chuẩn Android locale qualifier
2. Convert folder -> locale model nội bộ.
3. Auto-select những locale nào đang tồn tại trong ít nhất 1 module.
4. Có nút `Detect Existing` để refresh lại.

Ảnh của bạn đang có UX `Detect Existing` ở panel ngôn ngữ; rất nên giữ nguyên. fileciteturn1file7

### Phase D. Parse keys cần sync

1. Đọc text area keys.
2. Tách theo:
   - newline
   - comma
   - semicolon nếu muốn
3. Trim, distinct, bỏ rỗng.
4. Nếu user không nhập gì:
   - hoặc coi là `all keys from sheet`
   - hoặc báo lỗi yêu cầu nhập ít nhất 1 key

Khuyến nghị:

- để checkbox `If empty, sync all keys from sheet`

### Phase E. Preview changes

1. Với từng module được chọn.
2. Với từng locale được chọn.
3. Với từng key được chọn.
4. Đọc existing `strings.xml`.
5. Tính ra action:
   - `ADD`
   - `UPDATE`
   - `SKIP_ALREADY_EXISTS`
   - `SKIP_MISSING_IN_SHEET`
   - `ERROR_PLACEHOLDER_MISMATCH`
6. Show preview dạng table.

Ví dụ columns:

- Module
- Locale
- Key
- Action
- Old Value
- New Value
- File

### Phase F. Apply changes

1. Chạy write action/background task.
2. Tạo file nếu chưa tồn tại.
3. Chèn hoặc update node `<string name="...">`.
4. Ghi file.
5. Refresh VFS/Project view.
6. Show summary notification.

Summary nên có:

- X modules processed
- Y files changed
- Z keys added
- N keys updated
- K keys skipped
- errors count

---

## 8) Các quyết định kỹ thuật Codex phải theo

### 8.1 UI

Dùng Swing + IntelliJ UI components, không dùng JavaFX.

JetBrains có hẳn bộ custom UI components để plugin trông đồng nhất với IDE. citeturn577939search18

### 8.2 Settings

- URL mặc định nên được lưu qua `PersistentStateComponent`
- Có thể thêm 1 Settings page riêng trong IDE:
  - `Tools > String Sync` hoặc `Settings > Tools > String Sync`
- Trong settings page, user có thể lưu default Apps Script URL

JetBrains docs về settings và persistence đi theo hướng này. citeturn577939search0turn577939search15

### 8.3 Threading

- Network call không chạy trên EDT
- File scan lớn không chạy trên EDT
- Ghi file phải đúng write action / command model của IntelliJ
- UI update quay lại EDT

### 8.4 Notification

Dùng notification group của IDE:

- success
- warning
- error

### 8.5 Error reporting

Lỗi phải chỉ rõ:

- module nào
- locale nào
- key nào
- nguyên nhân gì

Ví dụ:

- `Placeholder mismatch for key msg_i_am_s in locale vi-VN of module app`
- `Sheet is missing translation for locale fr-FR`
- `Target file not writable`

---

## 9) Các rule nghiệp vụ phải copy từ tool cũ

Codex phải giữ nguyên các rule này vì đó là phần tạo ra độ an toàn cho sync:

1. Key chỉ cho phép `a-z`, `0-9`, `_`. fileciteturn2file1
2. Chặn XML injection `<string` và `</resources>`. fileciteturn2file1
3. Escape XML đặc biệt trước khi ghi file. fileciteturn2file1
4. Placeholder của mọi locale phải giống English base. fileciteturn2file1
5. Tạo file `strings.xml` nếu chưa có. fileciteturn2file1
6. Support chọn subset key. fileciteturn2file0

### Rule nên bổ sung

7. Nếu một key có trong sheet nhưng locale target thiếu bản dịch:
   - cho phép skip có warning
   - không fail toàn bộ batch trừ khi user bật strict mode

8. Nếu cùng 1 locale map ra nhiều folder Android khác nhau:
   - phải thống nhất rule mapping

9. Nếu cùng key đã tồn tại nhưng là `translatable="false"` hoặc có attribute khác:
   - preview phải cảnh báo trước khi update

10. Nếu file không phải UTF-8 hoặc XML malformed:
   - preview cảnh báo, không write ngay

---

## 10) Locale mapping: phần rất quan trọng

Đây là chỗ plugin mới phải làm tốt hơn Gradle task cũ.

### Bản đầu tối giản

Map như sau:

- `en-US` -> `values`
- `vi-VN` -> `values-vi`
- `de-DE` -> `values-de`
- `fr-FR` -> `values-fr`

Đây là cách gần với task cũ và ship nhanh.

### Bản chuẩn hơn

Tạo `LocaleMapper` với 2 chế độ:

- **Simplified mode**: cắt về language code như tool cũ
- **Android-qualified mode**: map đúng Android resource qualifiers khi cần region/script

Khuyến nghị:

- ship bản đầu với simplified mode
- nhưng thiết kế code sao cho `LocaleMapper` có thể thay thế độc lập

---

## 11) Milestone triển khai để giao cho Codex

### Milestone 0 - Bootstrap plugin project

**Mục tiêu:** tạo skeleton plugin chạy được trong Android Studio/IntelliJ.

Deliverables:

- project Gradle plugin
- action trong menu Tools
- dialog trống mở được
- settings service lưu được URL test

Acceptance criteria:

- `Run IDE` mở được sandbox IDE
- menu action hoạt động
- dialog mở không lỗi
- restart IDE vẫn nhớ URL

JetBrains hiện mô tả cách chuẩn là tạo Gradle-based plugin project từ New Project Wizard; Plugin DevKit không còn bundled từ 2023.3 nên cần cài riêng. citeturn359311search3turn577939search10

### Milestone 1 - Module scanning

**Mục tiêu:** detect module Android/resource module và render list chọn module.

Deliverables:

- scan module
- detect `res` directory
- detect existing locale folders
- panel UI module selection

Acceptance criteria:

- project nhiều module scan đúng
- module không có res không hiện hoặc hiện disabled
- module panel refresh được

### Milestone 2 - Sheet loading + language list

**Mục tiêu:** load URL, parse JSON, hiển thị languages.

Deliverables:

- client gọi Apps Script
- parse JSON
- detect base English
- list target languages
- nút detect existing

Acceptance criteria:

- URL hợp lệ load được
- URL sai báo lỗi rõ
- language list khớp sheet
- existing languages auto-select được

### Milestone 3 - Key parsing + preview

**Mục tiêu:** paste keys, preview thay đổi.

Deliverables:

- parser keys multiline/comma
- sync mode UI
- preview table
- summary counts

Acceptance criteria:

- key rác bị lọc hoặc báo lỗi
- preview hiện add/update/skip đúng
- placeholder mismatch hiện rõ

### Milestone 4 - Apply changes

**Mục tiêu:** ghi file thật.

Deliverables:

- writer cho `strings.xml`
- add/update theo mode
- tạo file mới nếu thiếu
- refresh IDE files
- success/error notifications

Acceptance criteria:

- add đúng vào nhiều module
- add only missing không overwrite key cũ
- update mode update đúng value
- lỗi 1 file không làm crash toàn bộ plugin

### Milestone 5 - Hardening

**Mục tiêu:** làm plugin đủ ổn để dùng hằng ngày.

Deliverables:

- logging
- error collection panel
- import/export settings
- preview diff đẹp hơn
- tests

Acceptance criteria:

- chạy ổn trên project thực tế của bạn
- không block UI
- không mất dữ liệu file

---

## 12) Prompt mẫu để đưa cho Codex theo từng milestone

Bạn nên cho Codex làm từng chặng nhỏ. Đừng yêu cầu “viết full plugin” ngay từ đầu.

### Prompt 1 - Bootstrap

```text
Create an IntelliJ Platform plugin in Kotlin for Android Studio.
Use a Gradle-based plugin project.
Add a Tools menu action called "String Sync from Google Sheet".
When triggered, open a DialogWrapper-based dialog.
Add an application/project service using PersistentStateComponent to store the last used Google Apps Script URL.
Keep the code split into action, dialog, and settings service classes.
Do not implement network or file writing yet.
```

### Prompt 2 - Module scanning

```text
Extend the plugin to scan the current project modules and detect Android/resource modules.
For each module, detect available res directories and existing values/strings.xml language folders.
Render them in a checkbox list panel with buttons: Auto-detect, Deselect all, Remove all.
Show module name, res path, and detected locale count.
Keep the scanning logic in a dedicated AndroidModuleScanner/ResourceScanner service.
```

### Prompt 3 - Sheet loading

```text
Add a Google Apps Script URL field and a Load Languages button.
Call the endpoint with HTTP GET in a background thread.
Parse JSON in the format Map<String, Map<String, String>> where the outer key is locale code and the inner map is key -> translation.
Detect the base English locale automatically.
Populate a language selection panel with search, Select All, Deselect All, and Detect Existing buttons.
Persist the last successful URL.
```

### Prompt 4 - Keys + modes

```text
Add a keys text area that accepts comma-separated and newline-separated keys.
Add sync modes:
1) Add or update selected keys in selected languages
2) Add only missing keys
3) Preview only
Create model classes for SyncMode, SyncRequest, FileChangePreview, and SyncResult.
Do not write files yet.
```

### Prompt 5 - Preview engine

```text
Implement preview generation for Android strings.xml files.
For each selected module and selected language, inspect existing strings.xml and determine whether each requested key should be added, updated, skipped, or reported as an error.
Reuse validation rules:
- key must match ^[a-z0-9_]+$
- reject XML injection like <string or </resources>
- placeholder sets must match the base English translation
Show results in a preview table dialog.
```

### Prompt 6 - Apply changes

```text
Implement writing changes to Android strings.xml files.
If the target file does not exist, create a valid resources XML file first.
Support two write modes:
- add/update selected keys
- add only missing keys
Escape XML correctly.
Run file writes safely and refresh the IDE virtual file system afterwards.
Show success and error notifications with per-module summaries.
```

### Prompt 7 - Polish

```text
Refactor the plugin for maintainability.
Improve UI labels and validation messages.
Add a dry-run preview summary.
Add tests for key parsing, placeholder validation, XML escaping, and locale folder mapping.
Document the architecture in README.md.
```

---

## 13) Yêu cầu code review mà bạn nên bắt Codex tuân thủ

Khi Codex sinh code, bạn nên kiểm bằng checklist này:

### Kiến trúc

- Không nhét business logic vào dialog
- Network client tách riêng
- XML parser/writer tách riêng
- scanner tách riêng
- settings service riêng

### IDE safety

- Không chạy network trên EDT
- Không block UI khi scan module
- Ghi file đúng cách
- Error không nuốt im lặng

### Android correctness

- Xác định đúng module target
- Xác định đúng res folder
- Không phá format `strings.xml`
- Không ghi sai locale folder

### UX

- validation dễ hiểu
- preview rõ ràng
- actions không gây mơ hồ
- dùng lại URL cũ được

---

## 14) Test cases tối thiểu phải có

### Functional

1. URL hợp lệ, load được locale list.
2. URL sai, báo lỗi.
3. JSON thiếu English, báo lỗi.
4. Module có `values` nhưng chưa có `values-vi`, plugin tạo được file mới.
5. Key mới được add đúng.
6. Key cũ được update đúng trong mode add/update.
7. Mode only missing không overwrite value cũ.
8. Key có placeholder mismatch bị chặn.
9. Key có XML injection bị chặn.
10. Input key chứa khoảng trắng, newline, comma vẫn parse đúng.

### Project structure

11. Project 1 module hoạt động.
12. Project nhiều module hoạt động.
13. Module không phải Android bị bỏ qua.
14. Module có nhiều res source set không crash.

### UX

15. Detect Existing auto-select đúng language.
16. Restart IDE vẫn nhớ URL cũ.
17. Preview only không ghi file.

---

## 15) Roadmap ship nhanh nhất

Nếu mục tiêu là có plugin chạy được sớm để dùng thực chiến, thứ tự tốt nhất là:

### Bản v1

- DialogWrapper UI
- ô URL
- scan modules
- detect existing languages
- paste keys
- preview
- apply 2 mode:
  - add/update selected keys
  - add only missing keys
- reuse JSON contract hiện tại

### Bản v1.1

- settings page
- better summary
- better error reporting
- export log

### Bản v1.2

- locale mapping chuẩn hơn cho region/script
- preview diff tốt hơn
- support plurals/string-array nếu sau này cần

---

## 16) Những gì không nên làm ở bản đầu

Để tránh plugin bị quá lớn ngay từ đầu, chưa nên làm:

- sync plurals
- sync string-array
- sync comment/doc metadata
- hỗ trợ mọi loại qualifier Android phức tạp
- live sync theo file watcher
- direct write không preview

---

## 17) Khuyến nghị cuối cùng

### Hướng build tốt nhất

Bạn nên yêu cầu Codex tạo plugin theo hướng:

- **Kotlin**
- **Gradle-based IntelliJ Platform plugin**
- **DialogWrapper UI** cho màn hình sync chính citeturn577939search1
- **PersistentStateComponent service** để lưu URL và state gần nhất citeturn577939search0turn577939search15
- **Android module/resource scanning** tách thành service
- **Tái sử dụng toàn bộ rule validate của Gradle tool hiện tại** vì đây là phần đã đúng nghiệp vụ của bạn fileciteturn2file1

### Kết luận thực dụng

Plugin này hoàn toàn khả thi và rất hợp để chuyển từ task Gradle sang plugin dùng chung. Điểm quan trọng nhất là:

1. giữ nguyên rule validate từ tool cũ
2. tách code thành scanner / API client / parser / writer / UI
3. làm preview trước khi write
4. xử lý locale mapping tốt hơn bản Gradle hiện tại

---

## 18) Definition of Done

Chỉ coi là xong khi plugin làm được toàn bộ các việc sau trong project thật:

- user paste được Apps Script URL `/exec`
- plugin load được locale từ sheet
- plugin detect được module Android
- plugin auto-focus/auto-select được ngôn ngữ đã có trong file strings
- user paste được list key
- user chọn được mode add/update hoặc only missing
- plugin preview chính xác những gì sẽ đổi
- plugin ghi file thành công cho nhiều module
- plugin báo lỗi rõ khi placeholder/key/XML sai
- lần sau mở lại vẫn nhớ URL cũ

