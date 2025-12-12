// 모든 페이지에서 실행될 네비게이션 로드 함수
// Version: 2.0 (강제 캐시 무효화)
async function loadNavbar() {
    try {
        const response = await fetch('/navbar.html?v=3.0');
        const navbarHTML = await response.text();
        
        // body의 가장 첫 번째에 navbar 삽입
        document.body.insertAdjacentHTML('afterbegin', navbarHTML);
        
        // navbar가 DOM에 완전히 렌더링된 후 네비게이션 업데이트
        await new Promise(resolve => setTimeout(resolve, 100));
        await updateNavigation();
    } catch (error) {
        console.error('[navbar-loader] Failed to load navbar:', error);
    }
}

// 네비게이션 업데이트 - 로그인 상태에 따라 메뉴 표시/숨김
async function updateNavigation() {
    try {
        const response = await fetch('/api/status');
        const data = await response.json();
        
        console.log('[navbar-loader] updateNavigation - isLoggedIn:', data.isLoggedIn);
        
        // 드롭다운 메뉴 아이템들
        const dashboardItem = document.getElementById('dashboardItem');
        const profileItem = document.getElementById('profileItem');
        const separatorItem = document.getElementById('separatorItem');
        const loginItem = document.getElementById('loginItem');
        const signupItem = document.getElementById('signupItem');
        const logoutItem = document.getElementById('logoutItem');
        const chatLink = document.getElementById('chatLink');
        const dropdownName = document.getElementById('dropdownName');
        
        if (data.isLoggedIn) {
            // 로그인 상태
            console.log('[navbar-loader] User logged in');
            
            // 로그인한 사용자 메뉴 표시
            if (dashboardItem) dashboardItem.style.display = 'block';
            if (profileItem) profileItem.style.display = 'block';
            if (separatorItem) separatorItem.style.display = 'block';
            if (logoutItem) logoutItem.style.display = 'block';
            if (chatLink) chatLink.style.display = 'block';  // 채팅 링크 표시
            
            // 로그인/회원가입 숨김
            if (loginItem) loginItem.style.display = 'none';
            if (signupItem) signupItem.style.display = 'none';
            
            // 사용자 정보 로드
            try {
                const userRes = await fetch('/api/user-info');
                if (userRes.ok) {
                    const user = await userRes.json();
                    if (dropdownName) {
                        dropdownName.textContent = user.full_name || user.username;
                        console.log('[navbar-loader] User info loaded:', user.full_name || user.username);
                    }
                }
            } catch (error) {
                console.error('[navbar-loader] Failed to load user info:', error);
                if (dropdownName) dropdownName.textContent = '사용자';
            }
        } else {
            // 로그아웃 상태
            console.log('[navbar-loader] User logged out');
            
            // 로그인한 사용자 메뉴 숨김
            if (dashboardItem) dashboardItem.style.display = 'none';
            if (profileItem) profileItem.style.display = 'none';
            if (separatorItem) separatorItem.style.display = 'none';
            if (logoutItem) logoutItem.style.display = 'none';
            if (chatLink) chatLink.style.display = 'none';  // 채팅 링크 숨김
            
            // 로그인/회원가입 표시
            if (loginItem) loginItem.style.display = 'block';
            if (signupItem) signupItem.style.display = 'block';
            
            // 드롭다운 이름 초기화
            if (dropdownName) dropdownName.textContent = '메뉴';
        }
    } catch (error) {
        console.error('[navbar-loader] Failed to check login status:', error);
    }
}

// 드롭다운 토글
function toggleDropdown(e) {
    e.preventDefault();
    e.stopPropagation();
    const dropdownMenu = document.getElementById('dropdownMenu');
    if (dropdownMenu) {
        dropdownMenu.classList.toggle('show');
        console.log('[navbar-loader] Dropdown toggled, show:', dropdownMenu.classList.contains('show'));
    }
}

// 로그아웃
function logout(e) {
    e.preventDefault();
    console.log('[navbar-loader] Logout clicked');
    fetch('/logout', { method: 'POST' })
        .then(() => {
            console.log('[navbar-loader] Logout successful, redirecting...');
            window.location.href = '/';
        })
        .catch(err => console.error('[navbar-loader] Logout error:', err));
}

// 앵커 네비게이션 처리
function handleAnchorNav(e, sectionId) {
    e.preventDefault();
    e.stopPropagation();
    console.log('[navbar-loader] handleAnchorNav called with:', sectionId);
    
    if (window.location.pathname !== '/') {
        // 다른 페이지면 홈으로 이동 후 앵커로 스크롤
        console.log('[navbar-loader] Not on home page, navigating to /#' + sectionId);
        window.location.href = '/#' + sectionId;
    } else {
        // 홈페이지면 해당 섹션으로 스크롤
        console.log('[navbar-loader] On home page, scrolling to section:', sectionId);
        const section = document.getElementById(sectionId);
        if (section) {
            section.scrollIntoView({ behavior: 'smooth' });
        } else {
            console.warn('[navbar-loader] Section not found:', sectionId);
        }
    }
}

// Hamburger 메뉴 토글
function toggleHamburgerMenu() {
    const navMenu = document.getElementById('navMenu');
    if (navMenu) {
        navMenu.classList.toggle('active');
        console.log('[navbar-loader] Hamburger menu toggled');
    }
}

// 문서 클릭 시 드롭다운 닫기
document.addEventListener('click', (e) => {
    const navbar = document.querySelector('nav.navbar');
    const dropdownMenu = document.getElementById('dropdownMenu');
    
    if (navbar && dropdownMenu && !navbar.contains(e.target)) {
        if (dropdownMenu.classList.contains('show')) {
            dropdownMenu.classList.remove('show');
            console.log('[navbar-loader] Dropdown closed by outside click');
        }
    }
});

// DOMContentLoaded 이벤트에서 navbar 로드
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadNavbar);
} else {
    // 이미 로드된 경우 바로 실행
    loadNavbar();
}
