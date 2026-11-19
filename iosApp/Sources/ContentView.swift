import SwiftUI
import Shared

/// Pure drawing, like the Compose screen: all decisions were made by the shared presenter.
struct ContentView: View {
    @StateObject private var viewModel: StoryListViewModel
    private let platform: String

    init(container: AppContainer) {
        // @StateObject keeps ONE view model alive for as long as this view is on screen
        // (Android equivalent: `by viewModels()`).
        _viewModel = StateObject(wrappedValue: StoryListViewModel(container: container))
        platform = container.platformLabel
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 8) {
                TextField("Search title, site or author", text: Binding(
                    get: { viewModel.state.query },
                    set: { viewModel.setQuery($0) }
                ))
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal)

                Picker("List", selection: Binding(
                    get: { viewModel.state.savedOnly },
                    set: { viewModel.setSavedOnly($0) }
                )) {
                    Text("Top").tag(false)
                    Text("Saved").tag(true)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                if viewModel.state.isLoading { ProgressView().progressViewStyle(.linear) }

                if let message = viewModel.state.errorMessage {
                    HStack {
                        Text(message).foregroundColor(.red).font(.footnote)
                        Spacer()
                        Button("Retry") { viewModel.refresh() }
                    }
                    .padding(.horizontal)
                }

                List(viewModel.state.items, id: \.id) { item in
                    StoryRow(item: item) { viewModel.toggleBookmark(item.id) }
                }
                .listStyle(.plain)

                // The one SHARED Compose card (same Kotlin code Android draws), hosted inside SwiftUI.
                SharedSummaryCard(
                    shown: Int32(viewModel.state.items.count),
                    saved: Int32(viewModel.state.items.filter { $0.isBookmarked }.count),
                    platform: platform
                )
                .frame(height: 64)
            }
            .navigationTitle("HN Reader")
            .toolbar {
                Button("Refresh") { viewModel.refresh() }
            }
        }
    }
}

private struct StoryRow: View {
    let item: StoryItem
    let onToggle: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                if let url = URL(string: item.url) {
                    Link(item.title, destination: url).font(.subheadline.weight(.semibold))
                } else {
                    Text(item.title).font(.subheadline.weight(.semibold))
                }
                Text("\(item.host) · \(item.meta)").font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            Button(action: onToggle) {
                Image(systemName: item.isBookmarked ? "star.fill" : "star")
            }
            .buttonStyle(.borderless)
        }
    }
}

/// Bridge: SwiftUI cannot draw Compose directly, so we host Compose's UIViewController.
/// UIViewControllerRepresentable is the standard "put a UIKit screen inside SwiftUI" adapter.
private struct SharedSummaryCard: UIViewControllerRepresentable {
    let shown: Int32      // Kotlin Int -> Swift Int32
    let saved: Int32
    let platform: String

    /// The coordinator keeps the Kotlin controller alive across SwiftUI redraws.
    final class Coordinator {
        let host: SummaryCardController
        init(platform: String) { host = SummaryCardController(platform: platform) }
    }

    func makeCoordinator() -> Coordinator { Coordinator(platform: platform) }

    func makeUIViewController(context: Context) -> UIViewController {
        context.coordinator.host.update(shown: shown, saved: saved)
        return context.coordinator.host.viewController
    }

    /// SwiftUI calls this whenever shown/saved change; Compose then recomposes by itself.
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        context.coordinator.host.update(shown: shown, saved: saved)
    }
}
