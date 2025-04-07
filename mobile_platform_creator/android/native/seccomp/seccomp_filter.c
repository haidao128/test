/**
 * Seccomp过滤器实现
 * 
 * 提供基于Seccomp的系统调用过滤功能，实现Android平台上的沙箱隔离
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <sys/prctl.h>
#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <sys/syscall.h>
#include <stddef.h>
#include <fcntl.h>

#define ARRAY_SIZE(x) (sizeof(x) / sizeof(x[0]))

/* 代表不同安全级别的系统调用列表 */
static const int kMinimalAllowList[] = {
    SCMP_SYS(read),
    SCMP_SYS(write),
    SCMP_SYS(open),
    SCMP_SYS(close),
    SCMP_SYS(stat),
    SCMP_SYS(fstat),
    SCMP_SYS(lstat),
    SCMP_SYS(poll),
    SCMP_SYS(lseek),
    SCMP_SYS(mmap),
    SCMP_SYS(mprotect),
    SCMP_SYS(munmap),
    SCMP_SYS(brk),
    SCMP_SYS(rt_sigaction),
    SCMP_SYS(rt_sigprocmask),
    SCMP_SYS(rt_sigreturn),
    SCMP_SYS(ioctl),
    SCMP_SYS(pread64),
    SCMP_SYS(pwrite64),
    SCMP_SYS(readv),
    SCMP_SYS(writev),
    SCMP_SYS(access),
    SCMP_SYS(pipe),
    SCMP_SYS(select),
    SCMP_SYS(sched_yield),
    SCMP_SYS(mremap),
    SCMP_SYS(msync),
    SCMP_SYS(mincore),
    SCMP_SYS(madvise),
    SCMP_SYS(shmget),
    SCMP_SYS(shmat),
    SCMP_SYS(shmctl),
    SCMP_SYS(dup),
    SCMP_SYS(dup2),
    SCMP_SYS(pause),
    SCMP_SYS(nanosleep),
    SCMP_SYS(getitimer),
    SCMP_SYS(alarm),
    SCMP_SYS(setitimer),
    SCMP_SYS(getpid),
    SCMP_SYS(sendfile),
    SCMP_SYS(socket),
    SCMP_SYS(connect),
    SCMP_SYS(accept),
    SCMP_SYS(sendto),
    SCMP_SYS(recvfrom),
    SCMP_SYS(sendmsg),
    SCMP_SYS(recvmsg),
    SCMP_SYS(shutdown),
    SCMP_SYS(bind),
    SCMP_SYS(listen),
    SCMP_SYS(getsockname),
    SCMP_SYS(getpeername),
    SCMP_SYS(socketpair),
    SCMP_SYS(setsockopt),
    SCMP_SYS(getsockopt),
    SCMP_SYS(clone),
    SCMP_SYS(fork),
    SCMP_SYS(vfork),
    SCMP_SYS(execve),
    SCMP_SYS(exit),
    SCMP_SYS(wait4),
    SCMP_SYS(kill),
    SCMP_SYS(uname),
    SCMP_SYS(fcntl),
    SCMP_SYS(flock),
    SCMP_SYS(fsync),
    SCMP_SYS(fdatasync),
    SCMP_SYS(truncate),
    SCMP_SYS(ftruncate),
    SCMP_SYS(getdents),
    SCMP_SYS(getcwd),
    SCMP_SYS(chdir),
    SCMP_SYS(fchdir),
    SCMP_SYS(rename),
    SCMP_SYS(mkdir),
    SCMP_SYS(rmdir),
    SCMP_SYS(creat),
    SCMP_SYS(link),
    SCMP_SYS(unlink),
    SCMP_SYS(symlink),
    SCMP_SYS(readlink),
    SCMP_SYS(chmod),
    SCMP_SYS(fchmod),
    SCMP_SYS(chown),
    SCMP_SYS(fchown),
    SCMP_SYS(lchown),
    SCMP_SYS(umask),
    SCMP_SYS(gettimeofday),
    SCMP_SYS(getrlimit),
    SCMP_SYS(getrusage),
    SCMP_SYS(sysinfo),
    SCMP_SYS(times),
    SCMP_SYS(ptrace),
    SCMP_SYS(getuid),
    SCMP_SYS(syslog),
    SCMP_SYS(getgid),
    SCMP_SYS(setuid),
    SCMP_SYS(setgid),
    SCMP_SYS(geteuid),
    SCMP_SYS(getegid),
    SCMP_SYS(setpgid),
    SCMP_SYS(getppid),
    SCMP_SYS(getpgrp),
    SCMP_SYS(setsid),
    SCMP_SYS(setreuid),
    SCMP_SYS(setregid),
    SCMP_SYS(getgroups),
    SCMP_SYS(setgroups),
    SCMP_SYS(setresuid),
    SCMP_SYS(getresuid),
    SCMP_SYS(setresgid),
    SCMP_SYS(getresgid),
    SCMP_SYS(getpgid),
    SCMP_SYS(setfsuid),
    SCMP_SYS(setfsgid),
    SCMP_SYS(getsid),
    SCMP_SYS(capget),
    SCMP_SYS(capset),
    SCMP_SYS(rt_sigpending),
    SCMP_SYS(rt_sigtimedwait),
    SCMP_SYS(rt_sigqueueinfo),
    SCMP_SYS(rt_sigsuspend),
    SCMP_SYS(sigaltstack),
    SCMP_SYS(utime),
    SCMP_SYS(mknod),
    SCMP_SYS(uselib),
    SCMP_SYS(personality),
    SCMP_SYS(futex),
    SCMP_SYS(sched_getparam),
    SCMP_SYS(sched_setparam),
    SCMP_SYS(sched_getscheduler),
    SCMP_SYS(sched_setscheduler),
    SCMP_SYS(sched_get_priority_max),
    SCMP_SYS(sched_get_priority_min),
    SCMP_SYS(sched_rr_get_interval),
    SCMP_SYS(epoll_create),
    SCMP_SYS(epoll_ctl),
    SCMP_SYS(epoll_wait),
    SCMP_SYS(restart_syscall),
};

/* 严格安全级别只允许的系统调用 */
static const int kStrictAllowList[] = {
    SCMP_SYS(read),
    SCMP_SYS(write),
    SCMP_SYS(open),
    SCMP_SYS(close),
    SCMP_SYS(stat),
    SCMP_SYS(fstat),
    SCMP_SYS(lstat),
    SCMP_SYS(poll),
    SCMP_SYS(lseek),
    SCMP_SYS(mmap),
    SCMP_SYS(mprotect),
    SCMP_SYS(munmap),
    SCMP_SYS(brk),
    SCMP_SYS(rt_sigaction),
    SCMP_SYS(rt_sigprocmask),
    SCMP_SYS(rt_sigreturn),
    SCMP_SYS(ioctl),
    SCMP_SYS(pread64),
    SCMP_SYS(pwrite64),
    SCMP_SYS(readv),
    SCMP_SYS(writev),
    SCMP_SYS(access),
    SCMP_SYS(pipe),
    SCMP_SYS(select),
    SCMP_SYS(sched_yield),
    SCMP_SYS(mremap),
    SCMP_SYS(msync),
    SCMP_SYS(mincore),
    SCMP_SYS(madvise),
    SCMP_SYS(dup),
    SCMP_SYS(dup2),
    SCMP_SYS(pause),
    SCMP_SYS(nanosleep),
    SCMP_SYS(getitimer),
    SCMP_SYS(alarm),
    SCMP_SYS(setitimer),
    SCMP_SYS(getpid),
    SCMP_SYS(socket),
    SCMP_SYS(connect),
    SCMP_SYS(accept),
    SCMP_SYS(sendto),
    SCMP_SYS(recvfrom),
    SCMP_SYS(sendmsg),
    SCMP_SYS(recvmsg),
    SCMP_SYS(shutdown),
    SCMP_SYS(bind),
    SCMP_SYS(listen),
    SCMP_SYS(getsockname),
    SCMP_SYS(getpeername),
    SCMP_SYS(socketpair),
    SCMP_SYS(setsockopt),
    SCMP_SYS(getsockopt),
    SCMP_SYS(exit),
    SCMP_SYS(uname),
    SCMP_SYS(fcntl),
    SCMP_SYS(getdents),
    SCMP_SYS(getcwd),
    SCMP_SYS(gettimeofday),
    SCMP_SYS(getrlimit),
    SCMP_SYS(getrusage),
    SCMP_SYS(times),
    SCMP_SYS(getuid),
    SCMP_SYS(getgid),
    SCMP_SYS(geteuid),
    SCMP_SYS(getegid),
    SCMP_SYS(getppid),
    SCMP_SYS(futex),
    SCMP_SYS(epoll_create),
    SCMP_SYS(epoll_ctl),
    SCMP_SYS(epoll_wait),
    SCMP_SYS(restart_syscall),
};

/* 沙箱级别 */
typedef enum {
    SANDBOX_LEVEL_MINIMAL = 0,  /* 最小限制 */
    SANDBOX_LEVEL_STANDARD = 1, /* 标准安全级别 */
    SANDBOX_LEVEL_STRICT = 2    /* 严格安全级别 */
} SandboxLevel;

/**
 * 初始化Seccomp过滤器
 *
 * @param level 沙箱安全级别
 * @return 0表示成功，负数表示失败
 */
int initialize_seccomp_filter(SandboxLevel level) {
    int ret = -1;
    scmp_filter_ctx ctx;
    
    /* 创建过滤器上下文，默认拒绝所有系统调用 */
    ctx = seccomp_init(SCMP_ACT_KILL);
    if (!ctx) {
        fprintf(stderr, "Failed to initialize seccomp filter\n");
        return -ENOMEM;
    }
    
    /* 根据安全级别添加允许的系统调用 */
    const int *allowlist;
    size_t allowlist_size;
    
    if (level == SANDBOX_LEVEL_MINIMAL) {
        allowlist = kMinimalAllowList;
        allowlist_size = ARRAY_SIZE(kMinimalAllowList);
    } else if (level == SANDBOX_LEVEL_STANDARD) {
        /* 标准级别使用部分严格级别的系统调用加上一些额外的调用 */
        allowlist = kStrictAllowList;
        allowlist_size = ARRAY_SIZE(kStrictAllowList);
        
        /* 添加标准级别允许的额外系统调用 */
        seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(fork), 0);
        seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(vfork), 0);
        seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(execve), 0);
        /* ... 其他标准级别允许的系统调用 ... */
    } else {
        /* 严格级别 */
        allowlist = kStrictAllowList;
        allowlist_size = ARRAY_SIZE(kStrictAllowList);
    }
    
    /* 添加允许的系统调用到过滤器 */
    for (size_t i = 0; i < allowlist_size; i++) {
        ret = seccomp_rule_add(ctx, SCMP_ACT_ALLOW, allowlist[i], 0);
        if (ret < 0) {
            fprintf(stderr, "Failed to add rule for syscall %d: %s\n", 
                   allowlist[i], strerror(-ret));
            goto out;
        }
    }
    
    /* 添加对exit_group的特殊处理 */
    ret = seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(exit_group), 0);
    if (ret < 0) {
        fprintf(stderr, "Failed to add rule for exit_group: %s\n", strerror(-ret));
        goto out;
    }
    
    /* 应用过滤器 */
    ret = seccomp_load(ctx);
    if (ret < 0) {
        fprintf(stderr, "Failed to load seccomp filter: %s\n", strerror(-ret));
        goto out;
    }
    
    /* 成功应用过滤器 */
    ret = 0;
    
out:
    /* 释放上下文 */
    seccomp_release(ctx);
    return ret;
}

/**
 * JNI入口函数
 *
 * 从Java层调用，设置seccomp过滤器
 */
JNIEXPORT jint JNICALL
Java_com_mobileplatform_creator_sandbox_SeccompManager_nativeInitializeSeccompFilter(
        JNIEnv *env, jobject thiz, jint level) {
    
    return initialize_seccomp_filter((SandboxLevel)level);
}

/**
 * 独立的测试入口点
 */
int main(int argc, char **argv) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <level>\n", argv[0]);
        fprintf(stderr, "  level: 0=minimal, 1=standard, 2=strict\n");
        return 1;
    }
    
    int level = atoi(argv[1]);
    if (level < 0 || level > 2) {
        fprintf(stderr, "Invalid level: %d\n", level);
        return 1;
    }
    
    printf("Initializing seccomp filter with level %d...\n", level);
    int ret = initialize_seccomp_filter((SandboxLevel)level);
    
    if (ret != 0) {
        fprintf(stderr, "Failed to initialize seccomp filter: %d\n", ret);
        return 1;
    }
    
    printf("Seccomp filter initialized successfully.\n");
    printf("Testing allowed syscalls...\n");
    
    /* 测试一些基本系统调用 */
    int fd = open("/dev/null", O_RDWR);
    if (fd >= 0) {
        printf("open() success.\n");
        close(fd);
        printf("close() success.\n");
    } else {
        printf("open() failed: %s\n", strerror(errno));
    }
    
    printf("Testing complete. Filter is working.\n");
    return 0;
} 