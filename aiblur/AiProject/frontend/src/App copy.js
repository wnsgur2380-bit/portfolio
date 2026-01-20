import React, { useState, useEffect } from 'react';
import axios from 'axios'; // axios 불러오기
import './App.css';

function App() {

    // 백엔드 환영 메시지 저장
    const [helloMessage, setHelloMessage] = useState('');
    
    // 사용자가 선택한 비디오 파일을 저장할 state(값이나 속성)
    const [selectedFile, setSelectedFile] = useState(null);

    // 업로드 상태 메시지를 저장할 state
    const [uploadStatus, setUploadStatus] = useState('');

    // 처음 로드될 때 "Hello" 메시지 받아오기
    useEffect(() => {
      axios.get('http://127.0.0.1:8000/')
        .then(response => {
          setHelloMessage(response.data.Hello);
        })
        .catch(error => {
          console.error('백엔드 통신 오류:', error);
        });
    }, []);
  
    // 파일 선택 <input>이 변경될 때 호출될 함수
    const handleFileChange = (event) => {
      // 사용자가 선택한 파일 (files[0]이 첫 번째 파일)
      setSelectedFile(event.target.files[0]);
      setUploadStatus(''); // 새 파일 선택 시 상태 메시지 초기화
    };

    // "업로드" 버튼을 클릭할 때 호출될 함수
    const handleUpload = () => {
      if(!selectedFile) {
        alert('업로드할 비디오 파일을 먼저 선택하세요!');
        return;
      }

      // FormData 생성 -> 폼 데이터를 백엔드로 보낼 때 사용하는 객체
      const formData = new FormData();

      // 폼 데이터에 파일 담기
      formData.append('video', selectedFile);

      // 업로드 중 메시지 표시
      setUploadStatus('영상을 업로드 중입니다...');

      // axios를 사용해 "POST" 방식으로 FastAPI에 FormData 전송
      axios.post('http://127.0.0.1:8000/upload-video/', formData)
        .then(response => {
          if (response.data.error) {
            // 'error'가 있다면 실패로 처리
            console.error('업로드 실패 (서버 오류):', response.data.error);
            setUploadStatus(`업로드 실패: ${response.data.error}`);
          } else {
            // 'error'가 없고 'message'가 있다면 백엔드(FastAPI)로부터 성공 메시지를 받음
            console.log(response.data);
            setUploadStatus(`성공: ${response.data.message} (${response.data.filename})`);
          }
        })
        .catch(error => {
          // 업로드 중 오류 발생 시
          console.error('업로드 실패:', error);
          setUploadStatus('업로드 실패. 콘솔을 확인하세요.');
        });
    };

    return (
      <div className="App">
      <header className="App-header">
        {/* 백엔드 환영 메시지 표시 */}
        <p>백엔드 응답: <strong>{helloMessage}</strong></p>

        <hr style={{ width: '50%' }} />

        {/* --- 영상 업로드 UI --- */}
        <h3>보행 영상 업로드</h3>
        
        {/* 파일 선택 버튼 (비디오 파일만 받도록 accept="video/*" 설정) */}
        <input 
          type="file" 
          accept="video/*" 
          onChange={handleFileChange} 
        />
        
        {/* 업로드 실행 버튼 */}
        <button 
          onClick={handleUpload} 
          style={{ marginTop: '10px', fontSize: '16px' }}
        >
          업로드 시작
        </button>

        {/* 업로드 상태 메시지 표시 */}
        {uploadStatus && (
          <p style={{ marginTop: '20px', color: '#61DAFB' }}>
            {uploadStatus}
          </p>
        )}
        {/* --------------------- */}

      </header>
    </div>
    );
}

export default App;