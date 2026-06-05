import { Injectable, inject } from '@angular/core';
import * as rxjs from 'rxjs';
import { firstValueFrom, Observable } from 'rxjs';
import { first, map, switchMap } from 'rxjs/operators';
import { NetworkV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { Node } from '../models';
import { shareReplayReturnValue } from '../utils/decorators/share-replay-return-value';
import { AuthenticationService } from './authentication.service';
import { Repo } from '../api/models/repo';

type Repository = Repo;

interface NetworkRepositories {
    repositories: Repository[];
}

@Injectable({
    providedIn: 'root',
})
export class NetworkService {
    private networkV1 = inject(NetworkV1Service);
    private authentication = inject(AuthenticationService);

    @shareReplayReturnValue()
    getRepositories(): Observable<Repository[]> {
        return this.authentication.observeLoginInfo().pipe(
            first((login) => login.isValidLogin),
            switchMap(() => this.networkV1.getRepositories()),
            map((repos) => repos.repositories),
        );
    }

    getRepository(id: string): Observable<Repository | null> {
        return this.getRepositories().pipe(
            map((repositories) => {
                if (id === HOME_REPOSITORY) {
                    return repositories.find((r) => r.isHomeRepo) ?? null;
                } else {
                    return repositories.find((r) => r.id === id) ?? null;
                }
            }),
        );
    }

    getHomeRepository(): Observable<Repository> {
        return this.getRepositories().pipe(
            map((repositories) => repositories.find((r) => r.isHomeRepo)!),
        );
    }

    isHomeRepository(id: string): Observable<boolean> {
        return this.getRepository(id).pipe(map((repository) => repository?.isHomeRepo ?? false));
    }

    isFromHomeRepository(node: Node): Observable<boolean> {
        if (!node?.ref || node.ref.isHomeRepo) {
            return rxjs.of(true);
        } else {
            return this.isHomeRepository(node.ref.repo);
        }
    }

    getRepositoryOfNode(node: Node): Observable<Repository | null> {
        if (node.ref.isHomeRepo) {
            return this.getHomeRepository();
        } else {
            return this.getRepository(node.ref.repo);
        }
    }

    async allFromHomeRepo(nodes: Node[]) {
        if (!nodes) return true;
        const repositories = await firstValueFrom(this.getRepositories());
        for (let node of nodes) {
            if (!node.ref.isHomeRepo && !this.isHomeRepo(node.ref.repo, repositories)) return false;
        }
        return true;
    }

    private isHomeRepo(repositoryId: string, repositories: Repo[]) {
        if (repositoryId == HOME_REPOSITORY) return true;
        if (!repositories) return false;
        let repository = this.getRepositoryById(repositoryId, repositories);
        if (repository) {
            return repository.isHomeRepo;
        }
        return false;
    }

    private getRepositoryById(id: string, repositories: Repo[]) {
        let i = repositories.findIndex((r) => r.id === id);
        if (id == HOME_REPOSITORY) {
            i = repositories.findIndex((r) => r.isHomeRepo);
        }
        if (i == -1) return null;
        return repositories[i];
    }
}
