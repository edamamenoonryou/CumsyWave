from flask import Flask, request, jsonify
from flask_cors import CORS
import os

import numpy as np
import pandas as pd
import librosa

#コサイン類似度変換ライブラリ
from sklearn.metrics.pairwise import cosine_similarity

#Gmail送信ライブラリ
import smtplib
from email.mime.text import MIMEText
from email.utils import formatdate

app = Flask(__name__)
CORS(app)

UPLOAD_FOLDER = './uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)


@app.route('/')
def hello_world():
    return 'Flask server is running!'

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return 'No file part', 400
    
    file = request.files['file']
    
    if file.filename == '':
        return 'No selected file', 400
    
    #ファイル保存
    file_path = os.path.join(UPLOAD_FOLDER, file.filename)
    file.save(file_path)

    #MFCC計算
    y, sr = librosa.load(file_path, sr=None) #音声ファイルの読み取り
    mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=12) #12個のMFCCを抽出
    new_mfcc = np.mean(mfccs.T, axis=0) #MFCCの平均をとる
    new_mfcc_numpy = np.asarray(new_mfcc)
    print(new_mfcc_numpy)

    return jsonify({"mfcc": new_mfcc_numpy.tolist()}), 200
    

@app.route('/mail', methods=['POST'])
def send_mail():
    print("mail")
    data = request.get_json()
    name = data.get('name')
    target_address = data.get('mail')

    send_address = 'mfcc23623@gmail.com'
    password = 'ewyz rkoo ldng bhjz'
    from_address = 'mfcc23623@gmail.com'
    target_subject = '確認メール'
    target_bodyText = f"""
        先ほど{name}さんに電話を掛けましたか？<br>
        身に覚えがない場合、急いで{name}さんに先ほどの電話はあなたを騙った詐欺の可能性があることを伝えてください。
        """
    smtpobj = smtplib.SMTP('smtp.gmail.com', 587)
    smtpobj.starttls()
    smtpobj.login(send_address, password)
    target_msg = MIMEText(target_bodyText, 'html')
    target_msg['Subject'] = target_subject
    target_msg['From'] = from_address
    target_msg['To'] = target_address
    target_msg['Date'] = formatdate()

    smtpobj.send_message(target_msg)
    smtpobj.close()

    return 201


@app.route('/family', methods=['POST'])
def send_family():
    print("family")
    data = request.get_json()
    name = data.get('name')
    target_address = data.get('mail')

    send_address = 'mfcc23623@gmail.com'
    password = 'ewyz rkoo ldng bhjz'
    from_address = 'mfcc23623@gmail.com'
    target_subject = '確認メール'
    target_bodyText = f"""
        先ほど{name}さんに声紋が誰とも一致しない電話が掛かってきました。<br>
        詐欺電話の可能性があるため、気をつけてください。
        """
    smtpobj = smtplib.SMTP('smtp.gmail.com', 587)
    smtpobj.starttls()
    smtpobj.login(send_address, password)
    target_msg = MIMEText(target_bodyText, 'html')
    target_msg['Subject'] = target_subject
    target_msg['From'] = from_address
    target_msg['To'] = target_address
    target_msg['Date'] = formatdate()

    smtpobj.send_message(target_msg)
    smtpobj.close()

    return 201


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)