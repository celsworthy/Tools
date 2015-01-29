import xlrd
import xlwt
import hashlib
import subprocess
import os
import sys
import tempfile

CELTECH_REPO_DIR="/home/tony/NetBeansProjects/celtechcore"
RESOURCES_DIR=os.path.join(CELTECH_REPO_DIR, "src", "main", "java", "celtech", "resources", "i18n")
TEMPLATES_PATH="/tmp/templates"
LANG_CODES=["de", "fi", "ko", "ru", "sv", "zh_CN", "zh_HK", "zh_SG", "zh_TW"]


class Row(object):

    def __init__(self):
        pass

    def fromSheet(self, sheet, rowNum):
        self.isValid = True
        try:
            self.rowNum = rowNum
            self.key = None
            self.english = sheet.cell_value(rowNum, 1)
            self.translation = sheet.cell_value(rowNum, 2)
            self.hash_ = sheet.cell_value(rowNum, 0)
        except Exception:
            self.isValid = False

    def fromLine(self, line):
        self.isValid = True
        try:
            self.rowNum = None
            self.key, self.english = line.split('=')
            self.translation = None
            self.hash_ = getHashForString(self.key)
        except Exception:
            self.isValid = False

    def __repr__(self):
        return "<Row %s K:%s H:%s E:%s T:%s>" % (self.rowNum, self.key, self.hash_, self.english, self.translation)


def getGitRepositoryFiles(tagOriginal, tagNew, filePath):
    os.chdir(CELTECH_REPO_DIR)

    hnd1, path1 = tempfile.mkstemp()
    hnd2, path2 = tempfile.mkstemp()

    subprocess.call(["git", "show", tagOriginal + ":" + filePath], stdout=hnd1)
    subprocess.call(["git", "show", tagNew + ":" + filePath], stdout=hnd2)

    os.close(hnd1)
    os.close(hnd2)

    return path1, path2


def getHashForString(keyString):
    # returning a cut down string with this hash method is safe and effective, according to Google
    # the length 10 can be increased in case of hash collisions (unlikely) and templates regenerated
    return hashlib.sha1(keyString).hexdigest()[:10]


def getChangedRows(rowsFileOriginal, rowsFileNew):
    """
    Return the changed or new rows, i.e. rows where the hash exists in 
    the new file but not the original
    """
    return


def getRowsFromLanguageFile(pathToLanguageFile):
    rowsByHash = {}
    for line in open(pathToLanguageFile, "r").readlines():
        line = line.strip()
        if len(line) > 0:
            row = Row()
            row.fromLine(line)
            if row.isValid:
                if row.hash_ in rowsByHash:
                    if row.key == rowsByHash[row.hash_].key:
                        print "ERROR: duplicate key found for " + row.key
                    else:    
                        raise Exception(
                            "Fatal Error - duplicate hash values found (%s, %s, %s) - suggest increase hash length"
                            % (row.hash_, row.key, rowsByHash[row.hash_].key))
                rowsByHash[row.hash_] = row
    return rowsByHash


def getLanguageFilesDelta(pathOriginal, pathNew):
    """
    Return one Row for each changed line in the language file, in a dict keyed by hash
    """
    rowsOriginal = getRowsFromLanguageFile(pathOriginal)
    rowsNew = getRowsFromLanguageFile(pathNew)
    # new rows will have a hash that does not exist in the original
    originalHashes = set(rowsOriginal.keys())
    newHashes = set(rowsNew.keys())
    changedHashes = newHashes.difference(originalHashes)
    deltaRows = {}
    for hash_ in changedHashes:
        deltaRows[hash_] = rowsNew[hash_]
    # add rows where english has changed for same key
    for hash_, row in rowsNew.iteritems():
        if hash_ in rowsOriginal and row.english != rowsOriginal[hash_].english:
            deltaRows[hash_] = row
    return deltaRows


def makeTemplateFileFromDeltaRows(deltaRows, pathTemplateXLS, languageCode):
    workbook = xlwt.Workbook(encoding="UTF-8")
    sheet = workbook.add_sheet("Translations - " + languageCode)

    headings = ["Hash", "English", "Translation"]
    rowx = 0
    for colx, value in enumerate(headings):
        sheet.write(rowx, colx, value)
    for row in deltaRows:
        rowx += 1
        sheet.write(rowx, 0, row.hash_)
        sheet.write(rowx, 1, row.english)

    sheet.set_panes_frozen(True) # frozen headings instead of split panes
    sheet.set_horz_split_pos(1)
    sheet.set_vert_split_pos(2)         

    workbook.save(pathTemplateXLS)


def getRowsFromXLS(pathToXLS):
    """
    Convert the contents of the file to a dictionary of Rows keyed
    on the hash.
    """
    rowsByHash = {}

    workbook = xlrd.open_workbook(pathToXLS)
    sheet = workbook.sheet_by_index(0)

    numRows = 3
    for rowNum in range(numRows):
        row = Row()
        row.fromSheet(sheet, rowNum)
        if row.isValid:
            rowsByHash[row.hash_] = row
    return rowsByHash


def makeTemplateFiles(tagOriginal, tagNew):
    path1, path2 = getGitRepositoryFiles(tagOriginal, tagNew, 
                         "src/main/java/celtech/resources/i18n/LanguageData.properties")
    deltaRowsByHash = getLanguageFilesDelta(path1, path2)
    for langCode in LANG_CODES:
        pathTemplateXLS = os.path.join(TEMPLATES_PATH, "LanguageData_" + langCode + ".xls")
        deltaTemplateXLS = makeTemplateFileFromDeltaRows(
            deltaRowsByHash.values(), pathTemplateXLS, langCode)
        

def updatePropertiesFileFromTemplate(propertiesPath, templateXLSPath):
    return



if __name__ == "__main__":
    makeTemplateFiles(sys.argv[1], sys.argv[2])
