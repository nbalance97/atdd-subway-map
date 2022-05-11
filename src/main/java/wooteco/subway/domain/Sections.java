package wooteco.subway.domain;

import java.util.*;
import java.util.stream.Collectors;

public class Sections {
    private final List<Section> sections;

    public Sections(Section section) {
        this.sections = new ArrayList<>(List.of(section));
    }

    public Sections(List<Section> sections) {
        this.sections = sections;
    }

    public void add(Section section) {
        validateDuplicate(section.getUpStationId(), section.getDownStationId());
        validateNoExist(section.getUpStationId(), section.getDownStationId());
        checkForkDownSection(section);
        checkForkUpSection(section);
        this.sections.add(section);
    }

    private void checkForkDownSection(Section section) {
        if (isDownSection(section) && isAnotherDownSection(section)) {
            Section currentSection = getAnotherDownSection(section);
            validateDistance(section, currentSection);
            addDownMiddleSection(section, currentSection);
        }
    }

    private void checkForkUpSection(Section section) {
        if (isUpSection(section) && isAnotherUpSection(section)) {
            Section currentSection = getAnotherUpSection(section);
            validateDistance(section, currentSection);
            addUpMiddleSection(section, currentSection);
        }
    }

    private void validateDistance(Section section, Section currentSection) {
        if (section.getDistance() >= currentSection.getDistance()) {
            throw new IllegalStateException("기존 구간보다 거리가 긴 구간을 입력할 수 없습니다.");
        }
    }

    private void addUpMiddleSection(Section section, Section currentSection) {
        this.sections.add(new Section(
                currentSection.getLineId(),
                currentSection.getUpStationId(),
                section.getUpStationId(),
                currentSection.getDistance() - section.getDistance()
        ));
        this.sections.remove(currentSection);
    }

    private boolean isAnotherUpSection(Section section) {
        return sections.stream()
                .filter(another -> section.getDownStationId().equals(another.getDownStationId()))
                .anyMatch(another -> !section.getUpStationId().equals(another.getUpStationId()));
    }

    private boolean isUpSection(Section section) {
        return sections.stream()
                .noneMatch(s -> s.isContainStationId(section.getUpStationId()));
    }

    private Section getAnotherUpSection(Section section) {
        return sections.stream()
                .filter(another -> section.getDownStationId().equals(another.getDownStationId()))
                .filter(another -> !section.getUpStationId().equals(another.getUpStationId()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("다른 구간을 찾을 수 없습니다."));
    }

    private void addDownMiddleSection(Section section, Section currentSection) {
        this.sections.add(new Section(
                currentSection.getLineId(),
                section.getDownStationId(),
                currentSection.getDownStationId(),
                currentSection.getDistance() - section.getDistance()
        ));
        this.sections.remove(currentSection);
    }

    private boolean isAnotherDownSection(Section section) {
        return sections.stream()
                .filter(another -> section.getUpStationId().equals(another.getUpStationId()))
                .anyMatch(another -> !section.getDownStationId().equals(another.getDownStationId()));
    }

    private Section getAnotherDownSection(Section section) {
        return sections.stream()
                .filter(another -> section.getUpStationId().equals(another.getUpStationId()))
                .filter(another -> !section.getDownStationId().equals(another.getDownStationId()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("다른 구간을 찾을 수 없습니다."));
    }

    private boolean isDownSection(Section section) {
        return sections.stream()
                .noneMatch(s -> s.isContainStationId(section.getDownStationId()));
    }

    public Set<Long> getStations() {
        final Set<Long> stations = new LinkedHashSet<>();
        Long stationId = getFirstStationId();

        while (checkSectionByUpStationId(stationId)) {
            Section section = getSectionByUpStationId(stationId);
            stations.add(section.getUpStationId());
            stations.add(section.getDownStationId());
            stationId = section.getDownStationId();
        }

        return stations;
    }

    private void validateDuplicate(Long upStationId, Long downStationId) {
        Set<Long> stationIds = getStations();
        if (stationIds.contains(upStationId) && stationIds.contains(downStationId)) {
            throw new IllegalStateException("상행과 하행 모두 이미 저장된 지하철역인 경우는 저장할 수 없습니다.");
        }
    }

    private void validateNoExist(Long upStationId, Long downStationId) {
        Set<Long> stationIds = getStations();
        if (!stationIds.contains(upStationId) && !stationIds.contains(downStationId)) {
            throw new IllegalStateException("상행과 하행 모두 저장되지 않은 지하철역인 경우 저장할 수 없습니다.");
        }
    }

    public void remove(Long stationId) {
        validateOneSection();
        validateNoExistStationId(stationId);
        final List<Section> removeCandidates = getSectionContainsStation(stationId);
        if (removeCandidates.size() == 1) {
            sections.remove(removeCandidates.get(0));
            return;
        }

        final Section upSection = getUpSection(stationId, removeCandidates);
        final Section downSection = getDownSection(stationId, removeCandidates);
        final Long lineId = upSection.getLineId();

        sections.add(new Section(
                lineId,
                downSection.getUpStationId(),
                upSection.getDownStationId(),
                downSection.getDistance() + upSection.getDistance()
        ));
        sections.remove(upSection);
        sections.remove(downSection);
    }

    private void validateNoExistStationId(Long stationId) {
        Set<Long> stationIds = getStations();
        if (!stationIds.contains(stationId)) {
            throw new IllegalStateException("저장되지 않은 지하철역을 제거할 수 없습니다.");
        }
    }

    private void validateOneSection() {
        if (sections.size() == 1) {
            throw new IllegalStateException("구간이 1개 남아있다면 삭제할 수 없습니다.");
        }
    }

    public Section getUpSection(Long stationId, List<Section> candidates) {
        return candidates.stream()
                .filter(candidate -> candidate.getUpStationId().equals(stationId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("상행 구간을 찾을 수 없습니다."));
    }

    public Section getDownSection(Long stationId, List<Section> candidates) {
        return candidates.stream()
                .filter(candidate -> candidate.getDownStationId().equals(stationId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("하행 구간을 찾을 수 없습니다."));
    }

    public List<Section> getSectionContainsStation(Long stationId) {
        return sections.stream()
                .filter(section -> section.isContainStationId(stationId))
                .collect(Collectors.toList());
    }

    private Long getFirstStationId() {
        return sections.stream()
                .filter(section -> checkFirstStation(section.getUpStationId()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("첫번째 역을 찾을 수 없습니다."))
                .getUpStationId();
    }

    private Section getSectionByUpStationId(Long upStationId) {
        return sections.stream()
                .filter(section -> section.getUpStationId().equals(upStationId))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("해당하는 Section을 찾을 수 없습니다."));
    }

    private boolean checkSectionByUpStationId(Long upStationId) {
        return sections.stream()
                .anyMatch(section -> section.getUpStationId().equals(upStationId));
    }

    private boolean checkFirstStation(Long upStationId) {
        return sections.stream()
                .noneMatch(section -> section.getDownStationId().equals(upStationId));
    }

    public List<Section> getSections() {
        return new ArrayList<>(sections);
    }
}