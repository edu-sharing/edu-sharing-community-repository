import { ClientConfig, HOME_REPOSITORY, MetadataSetInfo, Repository } from 'ngx-edu-sharing-api';

/**
 * Restricts the repositories offered to the user to the ones enabled via `availableRepositories`
 * in the client config.
 *
 * Note that the backend already filters the repository list by `searchable` and the
 * `TOOLPERMISSION_REPOSITORY_<appId>` toolpermission, so this is purely the config-level filter.
 */
export function filterRepositories(repositories: Repository[], config: ClientConfig): Repository[] {
    const enabledRepositories = config.availableRepositories;
    if (enabledRepositories) {
        return repositories.filter(
            (repo) =>
                (repo.isHomeRepo && enabledRepositories.includes(HOME_REPOSITORY)) ||
                enabledRepositories.includes(repo.id),
        );
    } else {
        return repositories;
    }
}

/**
 * Restricts the metadata sets offered for the given repository to the ones enabled via
 * `availableMds` in the client config.
 */
export function filterMetadataSets(
    metadataSets: MetadataSetInfo[],
    config: ClientConfig,
    repository: Repository,
): MetadataSetInfo[] {
    const enabledMetadataSets = config.availableMds?.find(
        (mdsConfig) =>
            mdsConfig.repository === repository.id ||
            (mdsConfig.repository === HOME_REPOSITORY && repository.isHomeRepo),
    )?.mds;
    if (enabledMetadataSets) {
        const mds = metadataSets.filter((mds) => enabledMetadataSets.includes(mds.id));
        if (mds.length === 0) {
            console.warn(
                `The filtered mds ${enabledMetadataSets} did not exists for the current app ${repository.id}. Check your Application XML config value "metadatasetsV2"`,
            );
        }
        return mds;
    } else {
        return metadataSets;
    }
}
