// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "ZeiterfassungApp",
    platforms: [
        .iOS(.v16),
        .macOS(.v13),
    ],
    products: [
        .library(
            name: "ZeiterfassungCore",
            targets: ["ZeiterfassungCore"]
        ),
    ],
    targets: [
        .target(
            name: "ZeiterfassungCore",
            path: "ZeiterfassungApp",
            exclude: [
                "App",
                "Views",
                "Resources",
            ]
        ),
        .testTarget(
            name: "ZeiterfassungAppTests",
            dependencies: ["ZeiterfassungCore"],
            path: "Tests"
        ),
    ]
)
