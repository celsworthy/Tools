import xlrd
import xlwt
import hashlib
import subprocess
import os
import tempfile

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
            self.hash_ = getHashForEnglish(self.english)
        except Exception:
            self.isVaid = False

    def fromLine(self, line):
        self.isValid = True
        try:
            self.rowNum = None
            self.key, self.english = line.split('=')
            self.translation = None
            self.hash_ = getHashForEnglish(self.english)
        except Exception:
            self.isVaid = False

    def __repr__(self):
        return "<Row %s %s %s %s>" % (self.rowNum, self.hash_, self.english, self.translation)


def getGitRepositoryFiles(tagOriginal, tagNew, filePath):
    os.chdir("/home/tony/NetBeansProjects/celtechcore")

    hnd1, path1 = tempfile.mkstemp()
    hnd2, path2 = tempfile.mkstemp()

    subprocess.call(["git", "show", tagOriginal + ":" + filePath], stdout=hnd1)
    subprocess.call(["git", "show", tagNew + ":" + filePath], stdout=hnd2)

    os.close(hnd1)
    os.close(hnd2)

    return path1, path2


def getHashForEnglish(englishString):
    # returning a cut down string with this hash method is safe and effective, according to Google
    return hashlib.sha1(englishString).hexdigest()[:10]


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
                rowsByHash[row.hash_] = row
    return rowsByHash


def getLanguageFilesDelta(pathOriginal, pathNew):
    rowsOriginal = getRowsFromLanguageFile(pathOriginal)
    rowsNew = getRowsFromLanguageFile(pathNew)
    # changed and new rows will have a hash that does not exist in the original
    originalHashes = set(rowsOriginal.keys())
    newHashes = set(rowsNew.keys())
    changedHashes = newHashes.difference(originalHashes)
    deltaRows = {}
    for hash_ in changedHashes:
        deltaRows[hash_] = rowsNew[hash_]
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
