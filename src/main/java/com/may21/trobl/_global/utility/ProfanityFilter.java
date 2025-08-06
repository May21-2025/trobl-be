package com.may21.trobl._global.utility;

import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class ProfanityFilter {

    private final Object patternLock = new Object();
    private Set<String> profanityWords;
    private Map<String, String> leetSpeakMap;
    private volatile Pattern profanityPattern;

    @PostConstruct
    public void init() {
        initializeProfanityWords();
        initializeLeetSpeakMap();
        buildProfanityPattern();
    }

    /**
     * 욕설 단어 목록 초기화
     */
    private void initializeProfanityWords() {
        profanityWords = ConcurrentHashMap.newKeySet();

        // 한국어 욕설
        profanityWords.addAll(
                Arrays.asList("바보", "멍청이", "개새끼", "시발", "씨발", "병신", "개놈", "년", "새끼", "좆", "꺼져",
                        "죽어", "미친", "염병", "지랄", "빡대가리", "한심한", "쓰레기", "썅", "개새", "개뻔뻔", "개소리",
                        "개같은", "좁밥", "좆같은", "시팔", "씨팔", "씨펄", "시펄", "시벌", "씨뻘", "ㅅㅂ", "ㅆㅂ", "ㅗ"));

        // 영어 욕설
        profanityWords.addAll(Arrays.asList("fuck", "shit", "bitch", "asshole", "dumbass", "idiot",
                "motherfucker", "whore", "damn", "crap", "bastard", "piss"));

        // 변형된 형태들 추가
        Set<String> variations = ConcurrentHashMap.newKeySet();
        for (String word : new HashSet<>(profanityWords)) {
            variations.addAll(generateVariations(word));
        }
        profanityWords.addAll(variations);
    }

    /**
     * 리트스피크(1337 speak) 매핑 초기화
     */
    private void initializeLeetSpeakMap() {
        leetSpeakMap = new ConcurrentHashMap<>();

        // 한글 자음/모음 변환
        leetSpeakMap.put("ㅅ", "s");
        leetSpeakMap.put("ㅂ", "b");
        leetSpeakMap.put("ㄱ", "g");
        leetSpeakMap.put("ㄴ", "n");
        leetSpeakMap.put("ㅇ", "o");
        leetSpeakMap.put("ㅡ", "-");
        leetSpeakMap.put("ㅗ", "ho");
        leetSpeakMap.put("ㅜ", "u");
        leetSpeakMap.put("ㅆ", "ss");

        // 숫자/기호 변환
        leetSpeakMap.put("1", "i");
        leetSpeakMap.put("3", "e");
        leetSpeakMap.put("4", "a");
        leetSpeakMap.put("5", "s");
        leetSpeakMap.put("7", "t");
        leetSpeakMap.put("0", "o");
        leetSpeakMap.put("@", "a");
        leetSpeakMap.put("!", "i");
    }

    /**
     * 욕설 패턴 빌드 (정규식) - Thread-safe
     */
    private void buildProfanityPattern() {
        synchronized (patternLock) {
            if (profanityWords.isEmpty()) {
                profanityPattern = Pattern.compile("(?!)");
                return;
            }

            StringBuilder patternBuilder = new StringBuilder();
            patternBuilder.append("(?i)");

            List<String> words = new ArrayList<>(profanityWords);
            for (int i = 0; i < words.size(); i++) {
                if (i > 0) patternBuilder.append("|");

                // 더 유연한 패턴 생성 - 각 글자 사이에 선택적 문자 삽입 허용
                String word = words.get(i);
                StringBuilder flexPattern = new StringBuilder();

                for (int j = 0; j < word.length(); j++) {
                    if (j > 0) {
                        // 글자 사이에 선택적으로 문자가 올 수 있음 (최대 2글자)
                        flexPattern.append("[가-힣a-zA-Z0-9.\\-_*!@#$%^&\\s]{0,2}?");
                    }
                    flexPattern.append(Pattern.quote(String.valueOf(word.charAt(j))));
                }

                patternBuilder.append("(?:^|\\s|[^가-힣a-zA-Z])")
                        .append(flexPattern.toString())
                        .append("(?=$|\\s|[^가-힣a-zA-Z])");
            }

            profanityPattern = Pattern.compile(patternBuilder.toString());
        }
    }

    /**
     * 단어의 변형 생성 대폭 개선
     */
    private Set<String> generateVariations(String word) {
        Set<String> variations = new HashSet<>();

        if (word.length() < 2) {
            return variations;
        }

        // 1. 띄어쓰기 변형
        for (int i = 1; i < word.length(); i++) {
            variations.add(word.substring(0, i) + " " + word.substring(i));
            variations.add(word.substring(0, i) + "  " + word.substring(i));
        }

        // 2. 특수문자 삽입
        String[] separators = {".", "-", "_", "*", "!", "@", "#", "$", "%", "^", "&"};
        for (String sep : separators) {
            for (int i = 1; i < word.length(); i++) {
                variations.add(word.substring(0, i) + sep + word.substring(i));
            }
        }

        // 3. 🔥 중간 문자 삽입 (핵심 개선) - "씨발" -> "씨이발", "씨아발" 등
        String[] commonInserts = {"이", "아", "어", "으", "우", "오", "에", "애", "을", "를"};
        for (String insert : commonInserts) {
            for (int i = 1; i < word.length(); i++) {
                variations.add(word.substring(0, i) + insert + word.substring(i));
            }
        }

        // 4. 숫자/영문 삽입 - "시발" -> "s1발", "시1발"
        String[] numInserts = {"1", "2", "3", "0", "a", "e", "i", "o", "u"};
        for (String insert : numInserts) {
            for (int i = 1; i < word.length(); i++) {
                variations.add(word.substring(0, i) + insert + word.substring(i));
            }
        }

        // 5. 반복 문자 개선
        for (int i = 0; i < word.length(); i++) {
            // 현재 문자 반복
            String repeated = word.substring(0, i + 1) + word.charAt(i) + word.substring(i + 1);
            if (repeated.length() <= word.length() + 2) {
                variations.add(repeated);
            }
        }

        // 6. 자음/모음 분리 - "씨발" -> "ㅆㅂ", "시발" -> "ㅅㅂ"
        if (word.matches(".*[가-힣].*")) {
            variations.addAll(generateConsonantVariations(word));
        }

        return variations;
    }

    /**
     * 자음 변형 생성
     */
    private Set<String> generateConsonantVariations(String word) {
        Set<String> variations = new HashSet<>();

        Map<String, String> syllableToConsonant = new HashMap<>();
        syllableToConsonant.put("씨", "ㅆ");
        syllableToConsonant.put("시", "ㅅ");
        syllableToConsonant.put("발", "ㅂ");
        syllableToConsonant.put("새", "ㅅ");
        syllableToConsonant.put("끼", "ㄲ");

        // 실제 변환 적용
        for (Map.Entry<String, String> entry : syllableToConsonant.entrySet()) {
            if (word.contains(entry.getKey())) {
                variations.add(word.replace(entry.getKey(), entry.getValue()));
            }
        }

        return variations;
    }

    /**
     * 텍스트 정규화 개선
     */
    private String normalizeText(String text) {
        if (text == null) return "";

        String normalized = text.toLowerCase()
                .trim();

        // 연속된 공백을 하나로 변경
        normalized = normalized.replaceAll("\\s+", " ");

        // 리트스피크 변환
        for (Map.Entry<String, String> entry : leetSpeakMap.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }

        // 특수문자 사이의 공백 제거
        normalized =
                normalized.replaceAll("([가-힣a-zA-Z])\\s*[.\\-_*!@#$%^&]\\s*([가-힣a-zA-Z])", "$1$2");

        // 연속된 같은 문자 정리 (ㅋㅋㅋㅋ -> ㅋ)
        normalized = normalized.replaceAll("(.)\\1{2,}", "$1");

        return normalized;
    }

    /**
     * 개선된 욕설 감지 - 유사도 기반 검사 추가
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.trim()
                .isEmpty()) {
            return false;
        }

        String normalizedText = normalizeText(text);

        // 1차: 정규식 패턴 검사
        Pattern currentPattern = profanityPattern;
        if (currentPattern != null && currentPattern.matcher(normalizedText)
                .find()) {
            return true;
        }

        // 2차: 유사도 기반 검사 (추가 보완)
        return checkSimilarity(normalizedText);
    }

    /**
     * 유사도 기반 욕설 검사
     */
    private boolean checkSimilarity(String text) {
        // 핵심 욕설 패턴들
        String[] corePatterns = {"시.*발", "씨.*발", "개.*새.*끼", "병.*신", "좆.*같", "지.*랄", "염.*병"};

        for (String pattern : corePatterns) {
            if (text.matches(".*" + pattern + ".*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 욕설을 별표(*)로 치환
     */
    public String filterProfanity(String text) {
        return filterProfanity(text, "*");
    }

    /**
     * 개선된 욕설 치환
     */
    public String filterProfanity(String text, String replacement) {
        if (text == null || text.trim()
                .isEmpty()) {
            return text;
        }

        String result = text;
        String normalizedText = normalizeText(text);

        // 1차: 직접 매칭되는 욕설 치환
        for (String profanity : profanityWords) {
            String regex = "(?i)" + Pattern.quote(profanity);
            if (normalizedText.contains(profanity.toLowerCase())) {
                result = result.replaceAll(regex,
                        replacement.repeat(Math.max(1, profanity.length())));
            }
        }

        // 2차: 패턴 기반 치환 (유사도 검사에서 걸린 것들)
        String[] corePatterns =
                {"(?i)(시.{0,2}발)", "(?i)(씨.{0,2}발)", "(?i)(개.{0,2}새.{0,2}끼)", "(?i)(병.{0,2}신)",
                        "(?i)(좆.{0,2}같)", "(?i)(지.{0,2}랄)"};

        for (String pattern : corePatterns) {
            result = result.replaceAll(pattern, "***");
        }

        return result;
    }

    /**
     * 욕설 단어 동적 추가
     */
    public void addProfanityWord(String word) {
        if (word == null || word.trim()
                .isEmpty()) {
            return;
        }

        String cleanWord = word.toLowerCase()
                .trim();
        profanityWords.add(cleanWord);
        profanityWords.addAll(generateVariations(cleanWord));
        buildProfanityPattern();
    }

    /**
     * 욕설 단어 제거
     */
    public void removeProfanityWord(String word) {
        if (word == null || word.trim()
                .isEmpty()) {
            return;
        }

        String cleanWord = word.toLowerCase()
                .trim();
        profanityWords.remove(cleanWord);

        Set<String> variations = generateVariations(cleanWord);
        profanityWords.removeAll(variations);

        buildProfanityPattern();
    }

    /**
     * 텍스트에서 발견된 욕설 목록 반환
     */
    public List<String> findProfanities(String text) {
        List<String> found = new ArrayList<>();
        if (text == null || text.trim()
                .isEmpty()) {
            return found;
        }

        String normalizedText = normalizeText(text);

        for (String profanity : profanityWords) {
            if (normalizedText.contains(profanity.toLowerCase())) {
                found.add(profanity);
            }
        }

        return found;
    }

    /**
     * 욕설 심각도 계산 (0.0 ~ 1.0)
     */
    public double calculateProfanitySeverity(String text) {
        if (text == null || text.trim()
                .isEmpty()) {
            return 0.0;
        }

        List<String> found = findProfanities(text);
        if (found.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        double maxPossibleScore = text.length();

        for (String profanity : found) {
            totalScore += profanity.length() * 1.5;
        }

        return Math.min(1.0, totalScore / maxPossibleScore);
    }

    /**
     * 종합적인 필터링 결과 반환
     */
    public FilterResult analyzeText(String text) {
        List<String> found = findProfanities(text);
        String filtered = filterProfanity(text);
        boolean contains = containsProfanity(text);
        double severity = calculateProfanitySeverity(text);

        return new FilterResult(text, filtered, contains, found, severity);
    }

    /**
     * 현재 등록된 욕설 단어 수 반환
     */
    public int getProfanityWordCount() {
        return profanityWords.size();
    }

    /**
     * 통계 정보 반환
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWords", profanityWords.size());
        stats.put("koreanWords", profanityWords.stream()
                .mapToLong(word -> word.matches(".*[가-힣].*") ? 1 : 0)
                .sum());
        stats.put("englishWords", profanityWords.stream()
                .mapToLong(
                        word -> word.matches(".*[a-zA-Z].*") && !word.matches(".*[가-힣].*") ? 1 : 0)
                .sum());
        return stats;
    }

    /**
     * 욕설 필터링 결과 정보를 담는 클래스
     */
    public static class FilterResult {
        @Getter
        private final String originalText;
        @Getter
        private final String filteredText;
        private final boolean containsProfanity;
        @Getter
        private final List<String> foundProfanities;
        @Getter
        private final double severity;

        public FilterResult(String originalText, String filteredText, boolean containsProfanity,
                List<String> foundProfanities, double severity) {
            this.originalText = originalText;
            this.filteredText = filteredText;
            this.containsProfanity = containsProfanity;
            this.foundProfanities = foundProfanities;
            this.severity = severity;
        }

        public boolean containsProfanity() {return containsProfanity;}

        @Override
        public String toString() {
            return String.format("FilterResult{containsProfanity=%s, severity=%.2f, found=%s}",
                    containsProfanity, severity, foundProfanities);
        }
    }

    // 테스트용 메인 메서드 추가
    public static void main(String[] args) {
        ProfanityFilter filter = new ProfanityFilter();
        filter.init();

        String[] testCases =
                {"씨이발", "시이발", "개새이끼", "시1발", "씨아발", "개새끼같은놈", "병1신", "좆같네", "지이랄", "안녕하세요",
                        "정말 좋아요"};

        System.out.println("=== 개선된 욕설 필터링 테스트 ===\n");
        for (String test : testCases) {
            FilterResult result = filter.analyzeText(test);
            System.out.printf("'%s' → 욕설: %s, 필터링: '%s'\n", test, result.containsProfanity(),
                    result.getFilteredText());
        }
    }
}
