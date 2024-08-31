import java.util.Map;
import java.util.Set;

public class JwtResponse {

    private Long userId;
    private Map<String, String> dsInfo;
    private Set<String> authorizations;
    private String accessToken;
    private String tokenType = "Bearer";

    public JwtResponse() {
    }

    public JwtResponse(Map<String, String> dsInfo, Set<String> authorizations, String accessToken) {
        this.dsInfo = dsInfo;
        this.authorizations = authorizations;
        this.accessToken = accessToken;
    }

    public JwtResponse(Long userId, Map<String, String> dsInfo, Set<String> authorizations, String accessToken) {
        this.userId = userId;
        this.dsInfo = dsInfo;
        this.authorizations = authorizations;
        this.accessToken = accessToken;
    }

    public Map<String, String> getDsInfo() {
        return dsInfo;
    }

    public void setDsInfo(Map<String, String> dsInfo) {
        this.dsInfo = dsInfo;
    }

    public Set<String> getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(Set<String> authorizations) {
        this.authorizations = authorizations;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
