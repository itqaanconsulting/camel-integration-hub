package nl.itqaanconsulting.integrationhub.order.persistence;

import nl.itqaanconsulting.integrationhub.order.domain.FileImportResult;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryFileImportStore {

    private final List<FileImportResult> imports = new CopyOnWriteArrayList<>();

    public void save(FileImportResult result) {
        imports.add(result);
    }

    public List<FileImportResult> findAll() {
        return imports.stream()
                .sorted(Comparator.comparing(FileImportResult::processedAt).reversed())
                .toList();
    }

    public void clear() {
        imports.clear();
    }
}
