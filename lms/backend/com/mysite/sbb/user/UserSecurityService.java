package com.mysite.sbb.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSecurityService implements UserDetailsService {

	private final UserRepository ur;

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		Optional<com.mysite.sbb.user.User> _user = ur.findByUserId(userId);

		if (_user.isEmpty()) {
			throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
		}

		com.mysite.sbb.user.User user = _user.get(); // (우리가 만든 User 엔티티)

		// 승인되지 않은 강사 로그인 차단
		if (user.getRole() == UserRole.ROLE_INSTRUCTOR && !user.isApproved()) {
			throw new RuntimeException("관리자 승인 시 로그인 및 이용 가능합니다.");
		}

		List<GrantedAuthority> authorities = new ArrayList<>();

		if (user.getRole() != null) {
			authorities.add(new SimpleGrantedAuthority(user.getRole().getKey()));
		}

		return new User(user.getUserId(), user.getPassword(), authorities);
	}
}