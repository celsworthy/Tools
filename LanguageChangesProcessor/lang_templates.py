"""
This module handles the import and export of language template XLS files which are used to detect changed
translation strings and apply updated translations.

USAGE:

$ python lang_templates.py EXPORT originalTag endTag deadlineDate

e.g. python lang_templates.py EXPORT 1.00.17 1.01.00 1/Feb/2015

where originalTag and endTag are e.g. git branches, tags, commits and deadlineDate must not have any spaces in it

which creates one .xls template file per language, each containing one row for each changed string

$ python lang_templates.py IMPORT

which, for each language code, updates the LanguageData_??.properties file in the repo with the changes in the .xls file

$ python lang_templates.py CHECK

which checks the translated properties files for missing entries and untranslated entries

$ python lang_templates.py FIX - DO NOT USE

which, for translated properties file, updates missing entries with the english text

$ python lang_templates.py COPY

copies the language files as per COPIES variable

$ python lang_templates.py MAKE_CHECK_TEMPLATES

creates a set of xls templates based on the data in the properties files so that they can be checked
by the translators


THE PROCESS IS:

The developer identifies the start and end commits across which changes were made to the LanguageData.properties file.

He then runs the EXPORT command, which
0) Extracts LanguageData.properties from Git for the two commits, and works out which keys are new and for which
keys the english has changed
1) Creates one XLS file for each LANG_CODE. The XLS file will contain one line for each new message and for
 messages where the english has changed.
2) If there is not currently any LanguageData_??.properties file for the requested language code, then one is
created by copying from LanguageData.properties, and the template XLS file is created with all rows from
LanguageData.properties and all translations blank.

After the XLS templates have been updated with the correct translations, the developer then runs the IMPORT command,
which updates the appropriate language properties files with the new translations in the XLS files. It will also
copy any language properties files as required, for instance the LanguageData_zh_HK.properties file will be copied
to the zh_TW and zh_SG versions.

Finally, after an IMPORT it is recommended to run a CHECK and then a FIX and a COPY.
"""

# location of celtechcore git repo
CELTECH_REPO_DIR = "/home/tony/NetBeansProjects/celtechcore"
# directory were templates are to be exported to and imported from
TEMPLATES_PATH = "/home/tony/tmp/templates"
# codes of languages to be exported / imported
LANG_CODES = ["en", "ja", "de", "fi", "ko", "ru", "sv", "zh_CN", "zh_HK", "fr", "es", "pl"]

# when sending out files to translators, the alias should be used in place of the lang code
ALIASES = {"ja":"Japanese", "cs": "Czech", "fr": "French", "es": "Spanish", "sv": "Swedish", "de": "German", "ko": "Korean", "ru": "Russian", "fi": "Finnish",
           "zh_CN": "Simplified Chinese", "zh_HK": "Traditional Chinese", "pl": "Polish",
           "nl": "Netherlands"}
# after updating language files, zh_HK properties file should be copied to zh_TW and zh_SG
COPIES = {"zh_HK" : ["zh_TW", "zh_SG"]}
#####################################
# LANG_CODES = ["nl"]
LANG_CODES = ["ja"]

RESTRICT_TO_KEYS=[
#    "versionWelcomeSubtitle1",   "versionWelcomeBody1",
#    "versionWelcomeSubtitle2",   "versionWelcomeBody2",
#    "versionWelcomeSubtitle3",   "versionWelcomeBody3",
]

import xlrd
import xlwt
import hashlib
import subprocess
import os
import sys
import tempfile
import shutil


def make_reverse_aliases(aliases):
    reverse_aliases = {}
    for code, alias in aliases.iteritems():
        reverse_aliases[alias] = code
    return reverse_aliases


RESOURCES_SUBDIR = os.path.join("src", "main", "java", "celtech", "resources", "i18n")
RESOURCES_DIR = os.path.join(CELTECH_REPO_DIR, RESOURCES_SUBDIR)
HOW_TO_EDIT_NOTE = """Please note, do NOT add or remove any rows to this spreadsheet. Only edit the translation column."""
REVERSE_ALIASES = make_reverse_aliases(ALIASES)


class Row(object):
    """
    The Row class represents both a row in a properties file and a row in an XLS file
    """

    def __init__(self):
        self.row_num = None
        self.key = None
        self.full_string = None
        self.translation = None
        self.hash_ = None
        self.is_valid = False

    def from_sheet(self, sheet, rowNum):
        self.is_valid = True
        try:
            self.row_num = rowNum
            self.key = None
            self.full_string = sheet.cell_value(rowNum, 1).encode('utf-8')
            self.translation = sheet.cell_value(rowNum, 2).encode('utf-8')
            self.hash_ = sheet.cell_value(rowNum, 0).encode('utf-8')
        except Exception:
            self.is_valid = False

    def from_jap_sheet(self, sheet, rowNum):
        # one-off for jacqui
        self.is_valid = True
        try:
            self.row_num = rowNum
            self.key = sheet.cell_value(rowNum, 0).encode('utf-8')
            self.full_string = sheet.cell_value(rowNum, 1).encode('utf-8')
            self.translation = sheet.cell_value(rowNum, 2).encode('utf-8')
            self.hash_ = get_hash_for_string(self.key)
        except Exception:
            self.is_valid = False

    def from_fre_sheet(self, sheet, rowNum):
        # one-off for jacqui
        self.is_valid = True
        try:
            self.row_num = rowNum
            self.key = None
            self.full_string = sheet.cell_value(rowNum, 0).encode('utf-8')
            self.translation = sheet.cell_value(rowNum, 1).encode('utf-8')
            self.hash_ = get_hash_for_string(self.full_string)
        except Exception:
            self.is_valid = False

    def from_line(self, line):
        self.is_valid = True
        try:
            self.row_num = None
            self.key, self.full_string = line.split('=')
            if self.key.startswith("#"):
                raise Exception("comment")
            self.translation = None
            self.hash_ = get_hash_for_string(self.key)
        except Exception:
            self.is_valid = False

    def write_to_properties_file(self, propertiesFile):
        full_string = convert_from_windows_line_endings(self.full_string)
        full_string = convert_unicode(full_string)
        line = "%s=%s%s" % (self.key, full_string, os.linesep)
        propertiesFile.write(line)

    def __repr__(self):
        return "<Row %s K:%s H:%s E:%s T:%s>" % (self.row_num, self.key, self.hash_, self.full_string, self.translation)


def convert_unicode(string):
    """
    Convert any \uxxxx to the correct unicode character
    """
    string = unicode(string, 'utf-8')
    import re
    converted = True
    while converted:
        regex = re.compile(r"\\u[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]")
        search = regex.search(string)
        if search != None:
            unicode_char_repr = string[search.start() + 2:search.end()]
            unicode_char = unichr(int(unicode_char_repr, 16))

            print "replace " + string[search.start():search.end()] + " with " + unicode_char
            string = string.replace(string[search.start():search.end()], unicode_char)
        else:
            converted = False
    return string.encode('utf-8')


def get_git_repository_files(tagOriginal, tagNew, filePath):
    os.chdir(CELTECH_REPO_DIR)

    hnd1, path1 = tempfile.mkstemp()
    hnd2, path2 = tempfile.mkstemp()

    subprocess.call(["git", "show", tagOriginal + ":" + filePath], stdout=hnd1)
    subprocess.call(["git", "show", tagNew + ":" + filePath], stdout=hnd2)

    os.close(hnd1)
    os.close(hnd2)

    shutil.copy2(path1, TEMPLATES_PATH + os.sep + "G1")
    shutil.copy2(path2, TEMPLATES_PATH + os.sep + "G2")

    return path1, path2


def get_hash_for_string(keyString):
    # returning a cut down string with this hash method is safe and effective, according to Google
    # the length 10 can be increased in case of hash collisions (unlikely) and templates regenerated
    return hashlib.sha1(keyString).hexdigest()[:10]


def get_rows_from_language_file(pathToLanguageFile):
    rows_by_hash = {}
    for line in open(pathToLanguageFile, "r").readlines():
        line = line.strip()
        if len(line) > 0:
            row = Row()
            row.from_line(line)
            if row.is_valid:
                if row.hash_ in rows_by_hash:
                    if row.key == rows_by_hash[row.hash_].key:
                        print "ERROR: duplicate key found for " + row.key
                    else:    
                        raise Exception(
                            "Fatal Error - duplicate hash values found (%s, %s, %s) - suggest increase hash length"
                            % (row.hash_, row.key, rows_by_hash[row.hash_].key))
                rows_by_hash[row.hash_] = row
    return rows_by_hash


def get_language_files_delta(path_original, path_new):
    """
    Return one Row for each changed line in the language file, in a dict keyed by hash
    """
    num = 0
    rows_original = get_rows_from_language_file(path_original)
    rows_new = get_rows_from_language_file(path_new)
    # new rows will have a hash that does not exist in the original
    original_hashes = set(rows_original.keys())
    new_hashes = set(rows_new.keys())
    changed_hashes = new_hashes.difference(original_hashes)
    delta_rows = {}
    for hash_ in changed_hashes:
        print "new key for " + rows_new[hash_].key
        delta_rows[hash_] = rows_new[hash_]
        num += 1
    # add rows where full_string has changed for same key
    num_changed_text = 0
    for hash_, row in rows_new.iteritems():

        if hash_ in rows_original and row.full_string != rows_original[hash_].full_string:
            print "changed key for " + rows_new[hash_].key
            print "old text: " + rows_original[hash_].full_string
            print "new text: " + row.full_string
            delta_rows[hash_] = row
            num_changed_text += 1
    print str(num_changed_text) + " changed text "
    print str(num) + " new rows "
    return delta_rows


def convert_to_windows_line_endings(unixString):
    """
    Convert r"\n" to CRLF
    """
    if (unixString is None):
        return ""
    return unixString.replace(r"\n", "\r\n")


def convert_from_windows_line_endings(windowsString):
    str = windowsString.replace("\r\n", r"\n")
    str = str.replace("\n", r"\n")
    str = str.replace("\r", r"\n")
    return str


def make_default_style():
    style = xlwt.XFStyle()
    alignment = xlwt.Alignment()
    alignment.wrap = True
    alignment.vert = xlwt.Alignment.VERT_TOP
    style.alignment = alignment
    return style


def make_edit_style():
    style = make_default_style()
    protection = xlwt.Protection()
    protection.cell_locked = False
    style.protection = protection
    return style


def make_template_file_from_delta_rows(deltaRows, pathTemplateXLS, languageCode, deadlineDate):
    workbook = xlwt.Workbook(encoding="UTF-8")
    sheet = workbook.add_sheet(ALIASES[languageCode])

    boldLargeFontStyle = xlwt.XFStyle()
    font = xlwt.Font()
    font.bold = True
    font.height = 400
    boldLargeFontStyle.font = font
    alignment = xlwt.Alignment()
    alignment.wrap = True
    alignment.vert = xlwt.Alignment.VERT_TOP
    boldLargeFontStyle.alignment = alignment

    headings = ["", "English", ALIASES[languageCode]]
    row_ix = 0
    sheet.write(row_ix, 1, HOW_TO_EDIT_NOTE, boldLargeFontStyle)
    sheet.write(row_ix, 2, "Deadline: " + deadlineDate, boldLargeFontStyle)
    row_ix += 1
    for col_ix, value in enumerate(headings):
        sheet.write(row_ix, col_ix, value, boldLargeFontStyle)
    for row in deltaRows:
        if len(RESTRICT_TO_KEYS) > 0:
            if not row.key in RESTRICT_TO_KEYS:
                continue
        row_ix += 1
        style = make_default_style()
        allowEditStyle = make_edit_style()
        sheet.write(row_ix, 0, row.hash_)
        sheet.write(row_ix, 1, convert_to_windows_line_endings(row.full_string), style)
        ############################################################
        # Only required for first iteration with Jacqui - normally translation should go out blank
        sheet.write(row_ix, 2, convert_to_windows_line_endings(row.translation), allowEditStyle)
        ############################################################
        numLines = 1 + row.full_string.count(r"\n") + len(row.full_string) / 60
        if numLines > 1:
            sheet.row(row_ix).height = 350 * numLines
            sheet.row(row_ix).height_mismatch = True

    sheet.row(0).height = 1500
    sheet.row(1).height = 500

    sheet.set_panes_frozen(True) # frozen headings instead of split panes
    sheet.set_horz_split_pos(2)
    sheet.set_vert_split_pos(2)      
    sheet.col(1).width = 256 * 80 # 80 columns approx
    sheet.col(2).width = 256 * 80

    sheet.protect = True

    workbook.save(pathTemplateXLS)


def get_rows_from_XLS(pathToXLS):
    """
    Convert the contents of the file to a dictionary of Rows keyed
    on the hash.
    """
    rowsByHash = {}

    workbook = xlrd.open_workbook(pathToXLS)
    sheet = workbook.sheet_by_index(0)

    START_ROW_IX = 2
    for rowNum in range(START_ROW_IX, sheet.nrows):
        row = Row()
        row.from_sheet(sheet, rowNum)
        if row.is_valid:
            rowsByHash[row.hash_] = row
    return rowsByHash


def update_delta_rows_with_latest_translations(deltaRows, lang_code):
    translationPropertiesFile = get_properties_file_path(lang_code)
    translationRows = get_rows_from_language_file(translationPropertiesFile)
    for row in deltaRows:
        row.translation = ""
        if row.hash_ in translationRows and len(translationRows[row.hash_].full_string) > 0:
            row.translation = translationRows[row.hash_].full_string


def make_new_language_properties_file(lang_code):
    """
    If a language code has been requested but there is no LanguageData_??.properties file for it,
    then create it by copying it from the LanguageData.properties file and clearing the translations
    """
    newPropertiesFilePath = get_properties_file_path(lang_code)
    engPropertiesFilePath = get_properties_file_path(None)
    shutil.copy(engPropertiesFilePath, newPropertiesFilePath)


def no_properties_file_for_lang_code(lang_code):
    """
    Return true if there is no properties for the give lang_code
    """
    propertiesFilePath = get_properties_file_path(lang_code)
    return not os.path.exists(propertiesFilePath)


def add_missing_entries_from_properties_file(lang_code, delta_rows_by_hash, path_to_second_revision):
    """
    For the given language, open the current .properties file and see which keys are missing
    compared to the latest english .properties file. Add a row for each missing key.
    """
    # print "add missing rows for language " + lang_code
    num_added = 0
    path_to_language_file = get_properties_file_path(lang_code)
    language_file_rows = get_rows_from_language_file(path_to_language_file)
    english_file_rows = get_rows_from_language_file(path_to_second_revision)
    for row_hash in english_file_rows:
        if row_hash not in language_file_rows:
            new_row = Row()
            new_row.hash_ = row_hash
            new_row.key = english_file_rows[row_hash].key
            # print "add row for " + english_file_rows[row_hash].full_string
            new_row.full_string = english_file_rows[row_hash].full_string
            delta_rows_by_hash[row_hash] = new_row
            num_added += 1
    print str(num_added) + " added for " + lang_code


def make_template_files(tagOriginal, tagNew, deadlineDate):
    path_to_first_revision, path_to_second_revision = get_git_repository_files(tagOriginal, tagNew,
                         os.path.join(RESOURCES_SUBDIR, "LanguageData.properties"))

    for lang_code in LANG_CODES:
        delta_rows_by_hash = get_language_files_delta(path_to_first_revision, path_to_second_revision)
        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + ALIASES[lang_code] + ".xls")
        if no_properties_file_for_lang_code(lang_code):
            make_new_language_properties_file(lang_code)
            delta_rows_by_hash = get_rows_from_language_file(path_to_second_revision)
            for row in delta_rows_by_hash.values():
                row.translation = ""
            make_template_file_from_delta_rows(delta_rows_by_hash.values(), pathTemplateXLS, lang_code, deadlineDate)
        else:
            # this should only be run on the initial iteration with Jacqui as usually the translation
            # should be left blank in the XLS
            update_delta_rows_with_latest_translations(delta_rows_by_hash.values(), lang_code)
            ########################################

            add_missing_entries_from_properties_file(lang_code, delta_rows_by_hash, path_to_second_revision)
            make_template_file_from_delta_rows(delta_rows_by_hash.values(), pathTemplateXLS, lang_code, deadlineDate)


def fill_in_english_from_delta_rows(deltaRows, lang_code):
    """
    Update the language properties file with the new english, so that a release can be made (if necessary)
    before the updated templates are returned
    """
    translationPropertiesFile = get_properties_file_path(lang_code)
    englishPropertiesFile = get_properties_file_path(None)
    translationRows = get_rows_from_language_file(translationPropertiesFile)
    englishRows = get_rows_from_language_file(englishPropertiesFile)
    for row in deltaRows:
        if row.hash_ not in translationRows:
            newRow = Row()
            newRow.key = row.key
            newRow.hash_ = row.hash_
            translationRows[row.hash_] = newRow

        translationRows[row.hash_].full_string = englishRows[row.hash_].full_string

    write_properties_file(translationPropertiesFile, translationRows.values())


def write_properties_file(path, rows):
    """
    Overwrite the properties file with the data in the given rows
    """
    rows.sort(key=lambda x: x.key)
    properties_file = open(path, "w+")
    for row in rows:
        row.write_to_properties_file(properties_file)
    properties_file.close()


def update_properties_file_from_template(propertiesPath, templateXLSPath, current_english_rows):
    propertiesRows = get_rows_from_language_file(propertiesPath)
    templateXLSRows = get_rows_from_XLS(templateXLSPath)
    for hash_, row in templateXLSRows.iteritems():
        if row.translation is None or len(row.translation) == 0:
            print "WARNING: Empty translation row while processing " + templateXLSPath + " hash: " + hash_
        else:
            if hash_ not in propertiesRows:
                propertiesRows[hash_] = Row()
                if hash_ in current_english_rows:
                    propertiesRows[hash_].key = current_english_rows[hash_].key
                else:
                    print "Not found in English file: " + hash_
            propertiesRows[hash_].full_string = row.translation

    write_properties_file(propertiesPath, propertiesRows.values())


def copy_language_files():
    """
    Copy the language files as specified in COPIES
    """
    for copy_from_lang_code, copy_to_list in COPIES.iteritems():
        if copy_from_lang_code == "":
            copy_from_lang_code = None
        copy_from_properties_file = get_properties_file_path(copy_from_lang_code)
        for copy_to_lang_code in copy_to_list:
            copy_to_properties_file = get_properties_file_path(copy_to_lang_code)
            shutil.copy(copy_from_properties_file, copy_to_properties_file)


def get_properties_file_path(lang_code):
    if lang_code is None:
        pathPropertiesFile = os.path.join(RESOURCES_DIR, "LanguageData.properties")
    else:
        pathPropertiesFile = os.path.join(RESOURCES_DIR, "LanguageData_" + lang_code + ".properties")
    return pathPropertiesFile


def import_template_files():
    current_english_rows = get_rows_from_language_file(get_properties_file_path(None))
    for lang_code in LANG_CODES:
        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + ALIASES[lang_code] + ".xls")
        pathPropertiesFile = get_properties_file_path(lang_code)
        update_properties_file_from_template(pathPropertiesFile, pathTemplateXLS, current_english_rows)
    copy_language_files()


def import_japanese_xls(path_to_xls):
    rowsByHash = {}
    workbook = xlrd.open_workbook(path_to_xls)
    sheet = workbook.sheet_by_index(0)

    START_ROW_IX = 3
    for rowNum in range(START_ROW_IX, sheet.nrows):
        row = Row()
        row.from_jap_sheet(sheet, rowNum)
        if row.is_valid:
            rowsByHash[row.hash_] = row
    return rowsByHash


def import_french_xls(path_to_xls):
    rowsByHash = {}
    workbook = xlrd.open_workbook(path_to_xls)
    sheet = workbook.sheet_by_index(0)

    START_ROW_IX = 3
    for rowNum in range(START_ROW_IX, sheet.nrows):
        row = Row()
        row.from_fre_sheet(sheet, rowNum)
        if row.is_valid:
            rowsByHash[row.hash_] = row
    return rowsByHash


def update_template_file_with_xls_data(translation_rows):
    template_xls_path = os.path.join(TEMPLATES_PATH, "LanguageData_Japanese.xls")
    template_xls_rows = get_rows_from_XLS(template_xls_path)
    for template_row in template_xls_rows.values():
        if template_row.hash_ in translation_rows:
            translation_row = translation_rows[template_row.hash_]
            template_row.translation = translation_row.translation
    make_template_file_from_delta_rows(template_xls_rows.values(), "/tmp/templates/LanguageData_Japanese.xls", "ja", "7/Mar/2015")


def update_template_file_with_xls_data_fr(translation_rows):
    template_xls_path = os.path.join(TEMPLATES_PATH, "LanguageData_French.xls")
    template_xls_rows = get_rows_from_XLS(template_xls_path)
    for template_row in template_xls_rows.values():
        template_hash = get_hash_for_string(template_row.full_string)
        if template_hash  in translation_rows:
            translation_row = translation_rows[template_hash]
            template_row.translation = translation_row.translation
    make_template_file_from_delta_rows(template_xls_rows.values(), "/tmp/templates/LanguageData_French.xls", "fr", "7/Mar/2015")


def update_ja_template_with_xls():
    # import the xls file that jacqui gave me
    rows = import_japanese_xls("/home/tony/japanese.xls")
    update_template_file_with_xls_data(rows)


def update_fr_template_with_xls():
    # import the xls file that jacqui gave me
    rows = import_french_xls("/home/tony/french.xls")
    update_template_file_with_xls_data_fr(rows)


def check_properties_files():
    """
    Check each foreign language file that  it
    (1) Has no blank lines (Warning)
    (2) The translation is not the same as the English (Warning)
    (3) Has all the entries in the English file (Warning)
    (4) Has no extra lines (Warning)
    """
    for lang_code in LANG_CODES:
        print "======================"
        print lang_code
        print "======================"
        translationPropertiesFile = get_properties_file_path(lang_code)
        englishPropertiesFile = get_properties_file_path(None)
        translationRows = get_rows_from_language_file(translationPropertiesFile)
        englishRows = get_rows_from_language_file(englishPropertiesFile)

        num_error_1 = 0
        num_error_2 = 0
        num_error_3 = 0
        for row in translationRows.values():
            if row.hash_ in englishRows:
                englishRow = englishRows[row.hash_]
            else:
                print "ERROR: no row in English file to match translation row " + row.hash_
                continue
            if row.full_string is None or len(row.full_string) == 0:
                # (1)
                print "WARNING: no translation while processing " + ": " + englishRow.key
                num_error_1 += 1
            if row.full_string == englishRow.full_string and not englishRow.full_string.startswith("*T") and not englishRow.full_string.upper() == "OKs":
                # (2)
                print "WARNING: row has not been translated: " + englishRow.key + ": " + englishRow.full_string
                num_error_2 += 1
        for englishRowHash in englishRows:
            if englishRowHash not in translationRows:
                print "ERROR: no translation found for row: " + englishRows[englishRowHash].key
                num_error_3 += 1
        print "======================"
        print lang_code
        print "No translation: " + str(num_error_1)
        print "Not translated: " + str(num_error_2)
        print "No translation for: " + str(num_error_3)


def fix_properties_files():
    """
    For each foreign language file, if it is missing an entry from the english file or the entry is empty,
    then copy in the english text.
    Also, update any unicode characters from \uxxxx to the actual unicode char
    """
    for lang_code in LANG_CODES:
        print "======================"
        print lang_code
        print "======================"
        translationPropertiesFile = get_properties_file_path(lang_code)
        englishPropertiesFile = get_properties_file_path(None)
        translationRows = get_rows_from_language_file(translationPropertiesFile)
        englishRows = get_rows_from_language_file(englishPropertiesFile)

        num_fixes_1 = 0
        num_fixes_2 = 0
        for row in translationRows.values():
            if row.hash_ in englishRows:
                englishRow = englishRows[row.hash_]
            else:
                print "ERROR: no row in English file to match translation row " + row.hash_
                continue
            if (row.full_string is None or len(row.full_string) == 0) and not (englishRow.full_string is None or len(englishRow.full_string) == 0):
                print "FIXING for key: " + englishRow.key
                row.full_string = englishRow.full_string
                num_fixes_1 += 1

        for englishRowHash in englishRows:
            if englishRowHash not in translationRows:
                print "ERROR: no translation found for row: " + englishRows[englishRowHash].key
                translationRows[englishRowHash] = englishRows[englishRowHash]
                num_fixes_2 += 1
        print "======================"
        print lang_code
        print "Empty translation: " + str(num_fixes_1)
        print "New keys: " + str(num_fixes_2)
        write_properties_file(translationPropertiesFile, translationRows.values())


def make_check_templates():
    """
    For each entry in LanguageData.properties and each language, create a template xls that contains
    the hash, English, and translation. This is so that the translators can check all translations.
    """
    for lang_code in LANG_CODES:
        print "======================"
        print lang_code
        print "======================"
        translationPropertiesFile = get_properties_file_path(lang_code)
        englishPropertiesFile = get_properties_file_path(None)
        translationRows = get_rows_from_language_file(translationPropertiesFile)
        englishRows = get_rows_from_language_file(englishPropertiesFile)
        for englishRow in englishRows.values():
            if englishRow.hash_ in translationRows:
                englishRow.translation = translationRows[englishRow.hash_].full_string

        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + ALIASES[lang_code] + ".xls")
        make_template_file_from_delta_rows(englishRows.values(), pathTemplateXLS, lang_code, "15/Mar/2015")


if __name__ == "__main__":
    if sys.argv[1] == "EXPORT":
        make_template_files(sys.argv[2], sys.argv[3], sys.argv[4])
    elif sys.argv[1] == "IMPORT":
        import_template_files()
    elif sys.argv[1] == "CHECK":
        check_properties_files()
    # elif sys.argv[1] == "FIX":
    #     fix_properties_files()
    elif sys.argv[1] == "COPY":
        copy_language_files()
    elif sys.argv[1] == "MAKE_CHECK_TEMPLATES":
        make_check_templates()
    elif sys.argv[1] == "JAP_IMPORT":
        # one-off job for Jacqui to update Japanese template file with contents of google apps spreadsheet
        update_ja_template_with_xls()
    elif sys.argv[1] == "FRA_IMPORT":
        # one-off job for Jacqui to update French template file with contents of google apps spreadsheet that does not have column A
        update_fr_template_with_xls()
