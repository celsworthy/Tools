"""
This module handles the import and export of language template XLS files which are used to detect changed
translation strings and apply updated translations.

USAGE:

$ python lang_templates.py EXPORT originalTag endTag

e.g. python lang_templates.py EXPORT 1.00.17 1.01.00

where originalTag and endTag are e.g. git branches, tags, commits

which creates one .xls template file per language, each containing one row for each changed string

$ python lang_templates.py IMPORT

which, for each language code, updates the LanguageData_??.properties file in the repo with the changes in the .xls file
"""

########## Configuration ############
# location of celtechcore git repo
CELTECH_REPO_DIR = "/home/tony/NetBeansProjects/celtechcore"
# directory were templates are to be exported to and imported from
TEMPLATES_PATH = "/tmp/templates"
# codes of languages to be exported / imported
LANG_CODES = ["de", "fi", "ko", "ru", "sv", "zh_CN", "zh_HK"]#, "fr", "es"]
LANG_CODES = ["ko"]

# when sending out files to translators, the alias should be used in place of the lang code
ALIASES = {"sv": "Swedish", "de": "German", "ko": "Korean", "ru": "Russian", "fi": "Finnish",
           "zh_CN": "Simplified Chinese", "zh_HK": "Traditional Chinese"}
# after updating language files, zh_HK properties file should be copied to zh_TW and zh_SG
COPIES = {"zh_HK" : ["zh_TW", "zh_SG"]}
#####################################

import xlrd
import xlwt
import hashlib
import subprocess
import os
import sys
import tempfile


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

    def from_line(self, line):
        self.is_valid = True
        try:
            self.row_num = None
            self.key, self.full_string = line.split('=')
            self.translation = None
            self.hash_ = get_hash_for_string(self.key)
        except Exception:
            self.is_valid = False

    def write_to_properties_file(self, propertiesFile):
        line = "%s=%s%s" % (self.key, convert_from_windows_line_endings(self.full_string), os.linesep)
        propertiesFile.write(line)

    def __repr__(self):
        return "<Row %s K:%s H:%s E:%s T:%s>" % (self.row_num, self.key, self.hash_, self.full_string, self.translation)


def get_git_repository_files(tagOriginal, tagNew, filePath):
    os.chdir(CELTECH_REPO_DIR)

    hnd1, path1 = tempfile.mkstemp()
    hnd2, path2 = tempfile.mkstemp()

    subprocess.call(["git", "show", tagOriginal + ":" + filePath], stdout=hnd1)
    subprocess.call(["git", "show", tagNew + ":" + filePath], stdout=hnd2)

    os.close(hnd1)
    os.close(hnd2)

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
    rows_original = get_rows_from_language_file(path_original)
    rows_new = get_rows_from_language_file(path_new)
    # new rows will have a hash that does not exist in the original
    original_hashes = set(rows_original.keys())
    new_hashes = set(rows_new.keys())
    changed_hashes = new_hashes.difference(original_hashes)
    delta_rows = {}
    for hash_ in changed_hashes:
        delta_rows[hash_] = rows_new[hash_]
    # add rows where full_string has changed for same key
    for hash_, row in rows_new.iteritems():
        if hash_ in rows_original and row.full_string != rows_original[hash_].full_string:
            delta_rows[hash_] = row
    return delta_rows


def convert_to_windows_line_endings(unixString):
    """
    Convert r"\n" to CRLF
    """
    return unixString.replace(r"\n", "\r\n")


def convert_from_windows_line_endings(windowsString):
    return windowsString.replace("\r\n", r"\n")


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


def update_delta_rows_with_latest_translations(deltaRows, langCode):
    translationPropertiesFile = os.path.join(RESOURCES_DIR, "LanguageData_" + langCode + ".properties")
    translationRows = get_rows_from_language_file(translationPropertiesFile)
    for row in deltaRows:
        row.translation = ""
        if row.hash_ in translationRows and len(translationRows[row.hash_].full_string) > 0:
            row.translation = translationRows[row.hash_].full_string


def make_template_files(tagOriginal, tagNew, deadlineDate):
    path1, path2 = get_git_repository_files(tagOriginal, tagNew,
                         os.path.join(RESOURCES_SUBDIR, "LanguageData.properties"))
    deltaRowsByHash = get_language_files_delta(path1, path2)

    for langCode in LANG_CODES:
        # this should only be run on the initial iteration with Jacqui as usually the translation
        # should be left blank in the XLS
        update_delta_rows_with_latest_translations(deltaRowsByHash.values(), langCode)

        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + ALIASES[langCode] + ".xls")
        make_template_file_from_delta_rows(deltaRowsByHash.values(), pathTemplateXLS, langCode, deadlineDate)

        fill_in_english_from_delta_rows(deltaRowsByHash.values(), langCode)


def fill_in_english_from_delta_rows(deltaRows, langCode):
    """
    Update the language properties file with the new english, so that a release can be made (if necessary)
    before the updated templates are returned
    """
    translationPropertiesFile = os.path.join(RESOURCES_DIR, "LanguageData_" + langCode + ".properties")
    englishPropertiesFile = os.path.join(RESOURCES_DIR, "LanguageData.properties")
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
    propertiesFile = open(path, "w+")
    for row in rows:
        row.write_to_properties_file(propertiesFile)
    propertiesFile.close()


def update_properties_file_from_template(propertiesPath, templateXLSPath):
    propertiesRows = get_rows_from_language_file(propertiesPath)
    templateXLSRows = get_rows_from_XLS(templateXLSPath)
    for hash_, row in templateXLSRows.iteritems():
        propertiesRows[hash_].full_string = row.translation
    write_properties_file(propertiesPath, propertiesRows.values())


def import_template_files():
    for langCode in LANG_CODES:
        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + ALIASES[langCode] + ".xls")
        pathPropertiesFile = os.path.join(RESOURCES_DIR, "LanguageData_" + langCode + ".properties")
        update_properties_file_from_template(pathPropertiesFile, pathTemplateXLS)


if __name__ == "__main__":
    if sys.argv[1] == "EXPORT":
        make_template_files(sys.argv[2], sys.argv[3], sys.argv[4])
    elif sys.argv[1] == "IMPORT":
        import_template_files()
