import xlrd
import xlwt
import hashlib


class Row(object):

    def __init__(self):
        pass

    def fromSheet(self, sheet, rowNum):
        self.isValid = True
        try:
            self.rowNum = rowNum
            self.english = sheet.cell_value(rowNum, 1)
            self.translation = sheet.cell_value(rowNum, 2)
            self.hash_ = getHashForEnglish(self.english)
        except Exception:
            self.isVaid = False

    def fromLine(self, line):
        self.isValid = True
        try:
            self.rowNum = None
            self.english, self.translation = line.split('=')
            self.hash_ = getHashForEnglish(self.english)
        except Exception:
            self.isVaid = False

    def __repr__(self):
        return "<Row %s %s %s %s>" % (self.rowNum, self.hash_, self.english, self.translation)


def getHashForEnglish(englishString):
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
